SELECT setval('reviews_id_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM reviews), false);
