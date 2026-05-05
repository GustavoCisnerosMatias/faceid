package com.facecompare.service;

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
public class FaceCompareService {

    private static final Logger LOG = LoggerFactory.getLogger(FaceCompareService.class);
    private static final double THRESHOLD = 0.363;

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
        LOG.info("FaceCompareService iniciado con YuNet + SFace");
    }

    public Result comparar(String imgBase64_1, String imgBase64_2) {
        Mat img1 = null;
        Mat img2 = null;
        try {
            img1 = decodificarBase64(imgBase64_1);
            img2 = decodificarBase64(imgBase64_2);

            if (img1 == null || img1.empty()) return Result.error("No se pudo leer imagen 1");
            if (img2 == null || img2.empty()) return Result.error("No se pudo leer imagen 2");

            EmbeddingResult r1 = embedding(img1, "imagen 1");
            EmbeddingResult r2 = embedding(img2, "imagen 2");

            if (!r1.ok() && !r2.ok()) return Result.error(r1.error() + " | " + r2.error());
            if (!r1.ok()) return Result.error(r1.error());
            if (!r2.ok()) return Result.error(r2.error());

            double similitud = coseno(r1.data(), r2.data());
            double porcentaje = Math.max(0.0, Math.min(100.0, Math.round(similitud * 1000.0) / 10.0));
            boolean esMismaPersona = similitud >= THRESHOLD;

            LOG.info("similitud={} porcentaje={}% mismaPersona={}", String.format("%.4f", similitud), porcentaje, esMismaPersona);
            return new Result(porcentaje, esMismaPersona, null);

        } catch (Exception e) {
            LOG.error("Error comparando imágenes: {}", e.getMessage(), e);
            return Result.error("Error interno: " + e.getMessage());
        } finally {
            if (img1 != null) img1.release();
            if (img2 != null) img2.release();
        }
    }

    private EmbeddingResult embedding(Mat img, String label) {
        try {
            FaceDetectorYN detector = FaceDetectorYN.create(
                    getYunetPath(), "", new Size(img.cols(), img.rows()), 0.5f, 0.3f, 5000);
            Mat faces = new Mat();
            detector.detect(img, faces);

            if (faces.rows() == 0) return EmbeddingResult.fail("No se detectó rostro en " + label);

            int best = 0;
            float bestScore = (float) faces.get(0, 14)[0];
            for (int i = 1; i < faces.rows(); i++) {
                float s = (float) faces.get(i, 14)[0];
                if (s > bestScore) { bestScore = s; best = i; }
            }

            FaceRecognizerSF recognizer = FaceRecognizerSF.create(getSfacePath(), "");
            Mat aligned = new Mat();
            recognizer.alignCrop(img, faces.row(best), aligned);

            Mat feat = new Mat();
            recognizer.feature(aligned, feat);

            float[] emb = new float[(int) feat.total()];
            feat.get(0, 0, emb);
            emb = normL2(emb);

            return embeddingValido(emb) ? EmbeddingResult.of(emb) : EmbeddingResult.fail("Embedding inválido en " + label);

        } catch (Exception e) {
            return EmbeddingResult.fail("Error en " + label + ": " + e.getMessage());
        }
    }

    private double coseno(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        return (na == 0 || nb == 0) ? 0.0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private float[] normL2(float[] v) {
        double sum = 0;
        for (float x : v) sum += x * x;
        double n = Math.sqrt(sum);
        if (n == 0) return v;
        float[] out = new float[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (float) (v[i] / n);
        return out;
    }

    private boolean embeddingValido(float[] e) {
        if (e == null || e.length == 0) return false;
        double mean = 0;
        for (float v : e) mean += v;
        mean /= e.length;
        double var = 0;
        for (float v : e) { double d = v - mean; var += d * d; }
        return (var / e.length) > 1e-6;
    }

    private Mat decodificarBase64(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        String clean = b64.contains(",") ? b64.split(",", 2)[1] : b64;
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
                if (yunetTempPath == null) yunetTempPath = toTemp(yunetResource, "yunet_", ".onnx");
            }
        }
        return yunetTempPath;
    }

    private String getSfacePath() {
        if (sfaceTempPath == null) {
            synchronized (sfaceLock) {
                if (sfaceTempPath == null) sfaceTempPath = toTemp(sfaceResource, "sface_", ".onnx");
            }
        }
        return sfaceTempPath;
    }

    private String toTemp(Resource resource, String prefix, String suffix) {
        try (InputStream is = resource.getInputStream()) {
            File temp = File.createTempFile(prefix, suffix);
            temp.deleteOnExit();
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return temp.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar modelo " + resource.getDescription(), e);
        }
    }

    public record Result(Double porcentaje, Boolean esMismaPersona, String error) {
        public static Result error(String msg) { return new Result(null, null, msg); }
        public boolean tieneError() { return error != null; }
    }

    private record EmbeddingResult(float[] data, String error) {
        boolean ok() { return data != null; }
        static EmbeddingResult of(float[] d) { return new EmbeddingResult(d, null); }
        static EmbeddingResult fail(String r) { return new EmbeddingResult(null, r); }
    }
}
