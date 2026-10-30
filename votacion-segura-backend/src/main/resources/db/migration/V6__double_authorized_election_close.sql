ALTER TABLE elecciones ADD COLUMN aprobador_cierre_id UUID REFERENCES usuarios(id);
