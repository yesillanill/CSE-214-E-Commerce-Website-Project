DO $$ 
DECLARE
  seq RECORD;
BEGIN
  FOR seq IN 
    SELECT c.relname AS seq_name,
           t.relname AS table_name,
           a.attname AS column_name
    FROM pg_class c
    JOIN pg_depend d ON d.objid = c.oid AND d.classid = 'pg_class'::regclass AND d.refclassid = 'pg_class'::regclass
    JOIN pg_class t ON t.oid = d.refobjid
    JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = d.refobjsubid
    WHERE c.relkind = 'S'
  LOOP
    EXECUTE 'SELECT setval(' || quote_literal(seq.seq_name) || ', COALESCE((SELECT MAX(' || quote_ident(seq.column_name) || ') FROM ' || quote_ident(seq.table_name) || '), 0) + 1, false)';
  END LOOP;
END $$;
