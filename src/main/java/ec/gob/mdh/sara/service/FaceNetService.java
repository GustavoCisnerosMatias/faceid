package ec.gob.mdh.sara.service;

import jakarta.annotation.PostConstruct;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.objdetect.FaceRecognizerSF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

@Service
public class FaceNetService {

    private static final Logger LOG = LoggerFactory.getLogger(FaceNetService.class);
    private static final double SFACE_THRESHOLD = 0.363;

    @Value("${facenet.yunet-path}")
    private Resource yunetResource;

    @Value("${facenet.sface-path}")
    private Resource sfaceResource;

    private volatile String yunetTempPath;
    private volatile String sfaceTempPath;
    private final Object yunetLock = new Object();
    private final Object sfaceLock = new Object();

    static {
        nu.pattern.OpenCV.loadLocally();
    }

    @PostConstruct
    public void init() {
        LOG.info("FaceNetService inicializando con YuNet + SFace (OpenCV DNN)...");
    }

    public ComparacionCompleta compararConDetalle(String imgB64_1, String imgB64_2) {
        Mat img1 = null;
        Mat img2 = null;
        try {
            img1 = decodificarBase64(imgB64_1);
            img2 = decodificarBase64(imgB64_2);

            if (img1 == null || img1.empty()) {
                return ComparacionCompleta.error("No se pudo decodificar imagen 1");
            }
            if (img2 == null || img2.empty()) {
                return ComparacionCompleta.error("No se pudo decodificar imagen 2");
            }

            String desc1 = String.format("imagen 1 (%dx%d)", img1.cols(), img1.rows());
            String desc2 = String.format("imagen 2 (%dx%d)", img2.cols(), img2.rows());

            EmbeddingResult res1 = obtenerEmbedding(img1, desc1);
            EmbeddingResult res2 = obtenerEmbedding(img2, desc2);

            if (!res1.ok() && !res2.ok()) {
                return ComparacionCompleta.error("Ambas imágenes fallaron — " + res1.error() + " | " + res2.error());
            }
            if (!res1.ok()) return ComparacionCompleta.error(res1.error());
            if (!res2.ok()) return ComparacionCompleta.error(res2.error());

            double similitud = similitudCoseno(res1.embedding(), res2.embedding());
            double porcentaje = Math.max(0.0, Math.min(100.0, Math.round(similitud * 1000.0) / 10.0));
            boolean esMismaPersona = similitud >= SFACE_THRESHOLD;

            LOG.info("Comparación facial -> similitud={} porcentaje={}% mismaPersona={}",
                    String.format("%.4f", similitud), porcentaje, esMismaPersona);

            return new ComparacionCompleta(porcentaje, esMismaPersona, null);

        } catch (Exception e) {
            LOG.error("Error en comparación: {}", e.getMessage(), e);
            return ComparacionCompleta.error("Error interno: " + e.getMessage());
        } finally {
            if (img1 != null) img1.release();
            if (img2 != null) img2.release();
        }
    }

    private EmbeddingResult obtenerEmbedding(Mat img, String descripcion) {
        try {
            FaceDetectorYN detector = FaceDetectorYN.create(
                    getYunetPath(), "", new Size(img.cols(), img.rows()), 0.5f, 0.3f, 5000);
            Mat faces = new Mat();
            detector.detect(img, faces);

            if (faces.rows() == 0) {
                return EmbeddingResult.fail("No se detectó rostro en " + descripcion);
            }

            int mejorIdx = 0;
            float mejorScore = (float) faces.get(0, 14)[0];
            for (int i = 1; i < faces.rows(); i++) {
                float s = (float) faces.get(i, 14)[0];
                if (s > mejorScore) { mejorScore = s; mejorIdx = i; }
            }

            FaceRecognizerSF recognizer = FaceRecognizerSF.create(getSfacePath(), "");
            Mat faceRow = faces.row(mejorIdx);
            Mat aligned = new Mat();
            recognizer.alignCrop(img, faceRow, aligned);

            Mat featMat = new Mat();
            recognizer.feature(aligned, featMat);

            float[] embedding = new float[(int) featMat.total()];
            featMat.get(0, 0, embedding);
            embedding = normalizarL2(embedding);

            if (!embeddingValido(embedding)) {
                return EmbeddingResult.fail("Embedding degenerado en " + descripcion);
            }
            return EmbeddingResult.of(embedding);

        } catch (Exception e) {
            LOG.error("Error procesando {}: {}", descripcion, e.getMessage());
            return EmbeddingResult.fail("Error en " + descripcion + ": " + e.getMessage());
        }
    }

    private double similitudCoseno(float[] a, float[] b) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private float[] normalizarL2(float[] vector) {
        double sum = 0.0;
        for (float v : vector) sum += v * v;
        double norm = Math.sqrt(sum);
        if (norm == 0.0) return vector;
        float[] out = new float[vector.length];
        for (int i = 0; i < vector.length; i++) out[i] = (float) (vector[i] / norm);
        return out;
    }

    private boolean embeddingValido(float[] embedding) {
        if (embedding == null || embedding.length == 0) return false;
        double mean = 0.0;
        for (float v : embedding) mean += v;
        mean /= embedding.length;
        double variance = 0.0;
        for (float v : embedding) { double d = v - mean; variance += d * d; }
        variance /= embedding.length;
        return variance > 1e-6;
    }

    private Mat decodificarBase64(String base64) {
        if (base64 == null || base64.isEmpty()) return null;
        String clean = base64.contains(",") ? base64.split(",", 2)[1] : base64;
        try {
            byte[] bytes = Base64.getDecoder().decode(clean);
            return Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
        } catch (IllegalArgumentException e) {
            byte[] bytes = Base64.getMimeDecoder().decode(clean);
            return Imgcodecs.imdecode(new MatOfByte(bytes), Imgcodecs.IMREAD_COLOR);
        }
    }

    private String getYunetPath() {
        if (yunetTempPath == null) {
            synchronized (yunetLock) {
                if (yunetTempPath == null) {
                    yunetTempPath = extraerRecursoATemporal(yunetResource, "yunet_", ".onnx");
                }
            }
        }
        return yunetTempPath;
    }

    private String getSfacePath() {
        if (sfaceTempPath == null) {
            synchronized (sfaceLock) {
                if (sfaceTempPath == null) {
                    sfaceTempPath = extraerRecursoATemporal(sfaceResource, "sface_", ".onnx");
                }
            }
        }
        return sfaceTempPath;
    }

    private String extraerRecursoATemporal(Resource resource, String prefix, String suffix) {
        try (InputStream is = resource.getInputStream()) {
            File temp = File.createTempFile(prefix, suffix);
            temp.deleteOnExit();
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo extraer recurso " + resource.getDescription() + ": " + e.getMessage(), e);
        }
    }

    public record ComparacionCompleta(
            Double porcentaje,
            Boolean esMismaPersona,
            String error
    ) {
        public static ComparacionCompleta error(String msg) {
            return new ComparacionCompleta(null, null, msg);
        }

        public boolean tieneError() {
            return error != null;
        }
    }

    private record EmbeddingResult(float[] embedding, String error) {
        boolean ok() { return embedding != null; }
        static EmbeddingResult of(float[] emb) { return new EmbeddingResult(emb, null); }
        static EmbeddingResult fail(String reason) { return new EmbeddingResult(null, reason); }
    }
}
