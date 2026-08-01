-- Categorias jurídicas base (conforme especificação, secção 80)
INSERT INTO categorias (id, nome, descricao) VALUES
    (gen_random_uuid(), 'Constitucional', 'Direito Constitucional e organização do Estado'),
    (gen_random_uuid(), 'Civil', 'Direito Civil'),
    (gen_random_uuid(), 'Penal', 'Direito Penal'),
    (gen_random_uuid(), 'Processual Penal', 'Processo Penal'),
    (gen_random_uuid(), 'Processual Civil', 'Processo Civil'),
    (gen_random_uuid(), 'Administrativo', 'Direito Administrativo'),
    (gen_random_uuid(), 'Família', 'Direito da Família'),
    (gen_random_uuid(), 'Trabalho', 'Direito do Trabalho'),
    (gen_random_uuid(), 'Fiscal', 'Direito Fiscal e Tributário'),
    (gen_random_uuid(), 'Organização Judiciária', 'Estrutura e funcionamento dos tribunais'),
    (gen_random_uuid(), 'Direitos Fundamentais', 'Direitos, liberdades e garantias fundamentais');
