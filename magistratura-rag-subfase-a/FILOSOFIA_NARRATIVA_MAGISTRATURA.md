# Filosofia narrativa — Magistratura

## Identidade

O sistema narrativo **não** é um curso de Direito disfarçado.
É o segundo pilar: formação de mentalidade por **aprendizagem invisível**.

## Regras absolutas

1. **Nunca** dizer “agora vais aprender X”, “a moral é…”, “o que aprendeste?”.
2. **Nunca** centrar tudo numa única personagem (ex.: João). Várias vidas, vários contextos.
3. Personagens **imperfeitas** no início; evolução natural, sem sermão.
4. O estudante só deve sentir que acompanha a vida de alguém.
5. Conhecimento jurídico fica no 1.º pilar (Biblioteca, Tutor, questões…).

## Estado actual do código

- Todas as histórias antigas (João) foram **apagadas** (`momentos: []`).
- UI e guia falam de **Experiências**, não de “Com o João”.
- A vista de experiência **não** mostra painéis de “o que percebeu” / pergunta didáctica.

## Como adicionar uma personagem

1. Regista em `PERSONAGENS` em `frontend/src/jornada/seed.js`.
2. Adiciona momentos com `personagemId`, `historia[]` (narrativa | dialogo).
3. Sem campos `oQueJoaoPercebeu`, `perguntaNaMente`, `desafio` pedagógico.
4. CTA opcional só como ponte (Biblioteca / Tutor), nunca como lição.

## Critério de aceite de qualquer feature

> “Esta decisão aproxima o estudante da forma de pensar de um magistrado?”

Se a resposta for não — não entra no projecto.
