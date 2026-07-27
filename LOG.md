# DevFlow Hub — Registo de Desenvolvimento

## Nota sobre a reorganização

Este LOG foi reconstruído de forma organizada com base no código-fonte existente, nos documentos das sprints, nos registos de desenvolvimento anteriores e nas validações realizadas.

O repositório anterior permanece como histórico do trabalho original. Este novo repositório organiza os principais marcos técnicos através de branches e commits coerentes.

As datas dos novos commits representam a data da reorganização no Git e não substituem as datas reais registadas nas sessões originais.

---

## Marco 0 — 17/07/2026 — Criação do novo repositório organizado

### Objetivo da sessão

Criar uma fundação limpa para o projeto DevFlow Hub, removendo dependências geradas, configurações pessoais e ficheiros desnecessários do novo histórico Git.

### Funcionalidades implementadas

- Criação de um novo repositório Git local.
- Definição da branch inicial `main`.
- Criação de um `.gitignore` adequado para Java, Maven, Node.js, Vite e IDEs.
- Criação de um `.gitattributes` para normalizar os finais de linha.
- Criação de um `.editorconfig` para uniformizar a formatação.
- Criação do README inicial.
- Criação do novo LOG de desenvolvimento.
- Definição da estratégia de branches.
- Preservação do código validado numa pasta-fonte separada.
- Exclusão da configuração local que contém a password do PostgreSQL.

### Problemas encontrados

- O repositório anterior continha ficheiros gerados dentro do histórico Git.
- A pasta `node_modules` tinha sido registada no controlo de versões.
- O projeto estava localizado dentro do OneDrive, causando bloqueios no npm e no Maven.
- O histórico anterior acumulava várias funcionalidades no mesmo branch.
- O README e o LOG já não estavam totalmente sincronizados com o código atual.

### Como resolvi

- Mantive o repositório anterior sem o eliminar.
- Copiei o código para uma pasta-fonte fora do OneDrive.
- Excluí `.git`, `node_modules`, `target`, `dist`, `.idea` e ficheiros locais.
- Criei o novo repositório em `C:\Dev\DevFlow_Hub`.
- Defini uma estratégia baseada em `main`, `develop` e branches de trabalho.
- Decidi adicionar o código progressivamente através de commits coerentes.
- Adicionei uma nota transparente sobre a reconstrução do histórico.

### Ficheiros atualizados

- `.gitignore`
- `.gitattributes`
- `.editorconfig`
- `README.md`
- `LOG.md`

### Estado atual

- Novo repositório inicializado.
- Branch `main` criada.
- Configuração base do Git preparada.
- Código-fonte original preservado em `C:\Dev\DevFlow_Hub_Source`.
- Nenhuma password local adicionada ao novo repositório.
- Nenhum ficheiro gerado adicionado ao Git.

### Validação ainda pendente

- Criar a branch `develop`.
- Adicionar o esquema PostgreSQL.
- Adicionar o backend progressivamente.
- Adicionar a API REST.
- Adicionar o frontend React.
- Adicionar a interface Thymeleaf.
- Adicionar os testes.
- Validar o JAR final.
- Criar o novo repositório remoto no GitHub.

### Próxima etapa

Criar a branch `develop` e iniciar a branch `feature/database-schema` para adicionar o esquema PostgreSQL e os dados de demonstração.

### Linhas de código escritas/alteradas — estimativa

120 linhas de configuração e documentação.
