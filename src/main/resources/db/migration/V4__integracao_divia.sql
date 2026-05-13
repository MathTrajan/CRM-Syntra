ALTER TABLE lead
    ADD COLUMN origem_externa VARCHAR(50),
    ADD COLUMN lead_externo_id VARCHAR(100);

CREATE UNIQUE INDEX uq_lead_origem_externa_id
    ON lead(origem_externa, lead_externo_id)
    WHERE origem_externa IS NOT NULL AND lead_externo_id IS NOT NULL;
