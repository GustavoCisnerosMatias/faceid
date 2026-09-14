-- 1. padron_rec_facial → agregar embedding para comparación rápida
ALTER TABLE MiesData.mies.padron_rec_facial
ADD embedding VARBINARY(512) NULL;

-- 2. padron_edu_rec_facial → mismo, para educadores
ALTER TABLE MiesData.mies.padron_edu_rec_facial
ADD embedding VARBINARY(512) NULL;

-- 3. rec_facial_Audit → agregar imagen para trazabilidad de fallos (beneficiarios)
ALTER TABLE MiesData.mies.rec_facial_Audit
ADD imagen VARBINARY(MAX) NULL;

-- 4. edu_rec_facial_Audit → mismo, para educadores
ALTER TABLE MiesData.mies.edu_rec_facial_Audit
ADD imagen VARBINARY(MAX) NULL;
-- Faltan estas dos para guardar embedding en historial
ALTER TABLE MiesData.mies.padron_audit_rec_facial     ADD embedding VARBINARY(512) NULL;
ALTER TABLE MiesData.mies.padron_edu_audit_rec_facial ADD embedding VARBINARY(512) NULL;

-- Para las _hist del siimies viejo
ALTER TABLE MiesData.mies.padron_rec_facial_hist     ADD embedding VARBINARY(512) NULL;
ALTER TABLE MiesData.mies.padron_edu_rec_facial_hist ADD embedding VARBINARY(512) NULL;