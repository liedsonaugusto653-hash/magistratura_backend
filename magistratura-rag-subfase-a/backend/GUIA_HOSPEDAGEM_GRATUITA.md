# 🚀 Guia de Hospedagem 100% Gratuita - Magistratura RAG

Este guia explica passo a passo como hospedar o seu sistema completo (Backend Java + Banco PostgreSQL com `pgvector` + Frontend) **sem gastar 1 centavo**.

---

## 🛠️ Arquitetura Gratuita Recomendada

1. **Banco de Dados PostgreSQL + PGVector**: **Supabase** (Plano Grátis)
   - *Por que?* Oferece PostgreSQL nativo com suporte a vetor (`pgvector`) totalmente gratuito sem necessidade de cartão de crédito.
2. **Backend (Spring Boot)**: **Render.com** ou **Koyeb** (Plano Web Service Grátis)
   - *Por que?* Suporta Docker, compila automaticamente via GitHub e executa serviços web de graça.
3. **Frontend (UX)**: **Vercel** ou **Render Static Sites** (Plano Grátis)
   - *Por que?* Hospedagem ilimitada, rápida e global para HTML/JS/CSS ou frameworks web.

---

## Passo 1: Configurar o Banco de Dados Gratuito (Supabase)

1. Acesse [supabase.com](https://supabase.com) e crie uma conta gratuita.
2. Clique em **"New Project"**.
3. Defina um nome para o projeto e crie uma **Senha Forte para o Banco de Dados** (Guarde essa senha!).
4. Escolha a região mais próxima (ex: *South America / São Paulo* ou *East US*).
5. Após a criação, vá em **Project Settings -> Database**:
   - Copie os dados de conexão (**Host**, **Database name**, **User**, **Port: 5432**).
   - O formato da JDBC URL será:
     `jdbc:postgresql://<SEU_HOST_SUPABASE>:5432/postgres?sslmode=require`

---

## Passo 2: Hospedar o Backend no Render.com

1. Suba o código atualizado do projeto para o seu repositório no **GitHub** (incluindo o `Dockerfile` e as alterações disponibilizadas no ZIP).
2. Acesse [render.com](https://render.com) e crie uma conta gratuita.
3. Clique em **New +** -> **Web Service**.
4. Conecte sua conta do GitHub e selecione o repositório do projeto.
5. Defina as configurações:
   - **Name**: `magistratura-backend`
   - **Environment**: `Docker`
   - **Instance Type**: `Free`
6. Adicione as seguintes **Environment Variables** (Variáveis de Ambiente):
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `SPRING_DATASOURCE_URL`: `jdbc:postgresql://<SEU_HOST_SUPABASE>:5432/postgres?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME`: `postgres`
   - `SPRING_DATASOURCE_PASSWORD`: `<SUA_SENHA_DO_SUPABASE>`
   - `JWT_SECRET`: `<UMA_CHAVE_SECRETA_LONGA_E_SEGURA>`
   - `GEMINI_API_KEY`: `<SUA_CHAVE_API_DO_GEMINI>` (Se aplicável)
7. Clique em **Deploy Web Service**.
8. Guarde a URL gerada pelo Render (ex: `https://magistratura-backend.onrender.com`).

---

## Passo 3: Hospedar o Frontend (UX) na Vercel ou Render

### Opção A: Vercel (Recomendado)
1. Acesse [vercel.com](https://vercel.com) e crie uma conta.
2. Importe o diretório `ux` ou a pasta do seu frontend a partir do GitHub.
3. Caso precise definir a URL da API do backend, adicione a variável de ambiente:
   - `NEXT_PUBLIC_API_URL` ou `VITE_API_URL`: `https://magistratura-backend.onrender.com`
4. Clique em **Deploy**.

---

## 📌 Arquivos Incluídos neste ZIP
- `Dockerfile`: Configuração multi-stage otimizada para publicar a aplicação Spring Boot no Render.
- `render.yaml`: Blueprint de implantação automatizada no Render.
- `src/main/resources/application-prod.yml`: Perfil de produção configurado para variáveis de ambiente e banco PostgreSQL.
- `GUIA_HOSPEDAGEM_GRATUITA.md`: Instruções detalhadas de implantação.
