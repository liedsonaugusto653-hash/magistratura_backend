-- Índices para pesquisa e desempenho

CREATE INDEX idx_utilizadores_email ON utilizadores (email);

CREATE INDEX idx_diplomas_titulo ON diplomas (lower(titulo));
CREATE INDEX idx_diplomas_numero ON diplomas (numero);
CREATE INDEX idx_diplomas_categoria ON diplomas (categoria_id);
CREATE INDEX idx_diplomas_titulo_trgm ON diplomas USING gin (to_tsvector('portuguese', titulo));

CREATE INDEX idx_artigos_diploma ON artigos (diploma_id);
CREATE INDEX idx_artigos_tema ON artigos (tema_id);
CREATE INDEX idx_artigos_texto_fts ON artigos USING gin (to_tsvector('portuguese', texto));

CREATE INDEX idx_resumos_diploma ON resumos (diploma_id);
CREATE INDEX idx_resumos_tema ON resumos (tema_id);

CREATE INDEX idx_flashcards_tema ON flashcards (tema_id);
CREATE INDEX idx_flashcards_categoria ON flashcards (categoria_id);
CREATE INDEX idx_flashcards_diploma ON flashcards (diploma_id);

CREATE INDEX idx_questoes_tema ON questoes (tema_id);
CREATE INDEX idx_questoes_categoria ON questoes (categoria_id);
CREATE INDEX idx_questoes_diploma ON questoes (diploma_id);

CREATE INDEX idx_simulado_questoes_simulado ON simulado_questoes (simulado_id);
CREATE INDEX idx_tentativas_utilizador ON tentativas_simulado (utilizador_id);
CREATE INDEX idx_tentativas_simulado ON tentativas_simulado (simulado_id);
CREATE INDEX idx_respostas_tentativa ON respostas_simulado (tentativa_id);

CREATE INDEX idx_historico_utilizador ON historico_estudo (utilizador_id);
CREATE INDEX idx_historico_data ON historico_estudo (data DESC);

CREATE INDEX idx_favoritos_utilizador ON favoritos (utilizador_id);

CREATE INDEX idx_conversas_utilizador ON conversas_ia (utilizador_id);
CREATE INDEX idx_mensagens_conversa ON mensagens_ia (conversa_id);
CREATE INDEX idx_mensagens_timestamp ON mensagens_ia (timestamp);
