# DevFlow Hub

DevFlow Hub é uma aplicação web académica para gestão de colaboradores, projetos, tarefas, tempo de trabalho e programas internos.

## Estado do repositório

Este repositório representa uma reorganização limpa e estruturada do projeto DevFlow Hub. O código foi desenvolvido anteriormente num repositório de trabalho e foi reorganizado para:

- remover ficheiros gerados, dependências instaladas e configurações locais;
- separar funcionalidades por branches e commits coerentes;
- manter o README e o LOG alinhados com o código;
- preparar uma entrega portátil com backend, frontend, SQL e documentação;
- preservar um histórico técnico claro para avaliação académica.

As datas dos commits reorganizados representam a organização técnica do novo repositório e não substituem as datas reais registadas no `LOG.md`.

## Arquitetura atual

```text
React + TypeScript + Vite
          |
          | HTTP, JSON e JWT Bearer
          v
Spring Boot REST API
          |
          v
PostgreSQL
```

O frontend React é responsável pela interface, navegação, autenticação no cliente e apresentação dos dados. O backend Spring Boot é responsável pela segurança, regras de negócio, validação, persistência e respostas da API.

A decisão arquitetural está documentada em [`docs/architecture/frontend-decision.md`](docs/architecture/frontend-decision.md).

## Tecnologias utilizadas

### Backend

- Java 21
- Spring Boot 4.0.6
- Spring MVC
- Spring Security e OAuth2 Resource Server
- Spring Data JPA
- Jakarta Validation
- Maven Wrapper

### Frontend

- React 19
- TypeScript 6
- Vite 8
- React Router 7
- Fetch API nativa
- ESLint

### Base de dados

- PostgreSQL
- H2 em memória para testes automatizados

### Testes

- JUnit 5
- Mockito
- MockMvc
- Spring Boot Test
- Smoke tests em PowerShell com PostgreSQL real
- ESLint e build TypeScript/Vite no frontend

## Estado atual

### Backend e API

- Estrutura PostgreSQL criada e validada numa base limpa.
- Scripts de instalação e migração disponíveis.
- Entidades JPA, repositories e services implementados.
- CRUD de colaboradores, projetos, tarefas e programas internos implementado.
- Temporizador de tarefas com início, pausa, retoma, conclusão e acumulação de tempo.
- Campos de auditoria das tarefas protegidos e geridos automaticamente.
- Endpoint agregado `GET /api/dashboard` implementado.
- Tratamento global de erros estruturados para HTTP `400`, `401` e `404`.
- JSON malformado rejeitado sem expor stack traces ou detalhes internos.
- Autenticação JWT implementada.
- Login público em `POST /api/auth/login`.
- Restantes endpoints em `/api/**` protegidos com Bearer token.
- Tokens com expiração de 900 segundos.
- Colaboradores inativos impedidos de iniciar sessão.
- Alteração de palavra-passe associada ao colaborador autenticado através da claim `sub`.
- Backend validado com PostgreSQL real e `ddl-auto=validate`.
- Suite backend validada com 38 testes sem falhas.

### Frontend React

- Fundação React, TypeScript e Vite implementada.
- Rotas `/login`, `/dashboard`, `/projects`, `/tasks` e página de recurso não encontrado.
- Rotas do dashboard, da lista de projetos e da lista de tarefas protegidas.
- Integração real com `POST /api/auth/login`.
- Sessão autenticada guardada em `sessionStorage`.
- Persistência da sessão após atualização da página.
- Logout manual e expiração automática da sessão.
- Cabeçalho `Authorization: Bearer <token>` aplicado aos pedidos autenticados.
- Tratamento global de respostas `401`, limpeza da sessão e redirecionamento para `/login`.
- Dashboard autenticado ligado a `GET /api/dashboard`.
- Indicadores reais de colaboradores, projetos, tarefas, programas e tempo registado.
- Distribuição de tarefas por estado.
- Apresentação de tarefas recentes e projetos com prazos próximos.
- Estados de carregamento, erro, repetição do pedido e ausência de dados.
- Interface responsiva.
- Cabeçalho autenticado reutilizável com identidade do utilizador, navegação e logout.
- Navegação entre Dashboard, Projects e Tasks através de React Router.
- Lista autenticada de projetos ligada a `GET /api/projects`.
- Dados dos gestores obtidos através de `GET /api/collaborators`.
- Projetos apresentados com estado, descrição, gestor e datas.
- Estados de carregamento, erro, repetição do pedido e lista vazia na página de projetos.
- Lista autenticada de tarefas ligada a `GET /api/tasks`.
- Projetos e responsáveis das tarefas resolvidos através de `GET /api/projects` e `GET /api/collaborators`.
- Tarefas apresentadas com estado, prioridade, projeto, responsável, tempo registado e datas de auditoria.
- Indicação do número de temporizadores ativos e preparação da atualização visual do tempo quando um temporizador estiver em execução.
- Estados de carregamento, erro, repetição do pedido e lista vazia na página de tarefas.
- Layout validado em desktop e numa viewport móvel de `390 × 844`.
- `npm run lint` e `npm run build` validados com sucesso.

### Trabalho ainda pendente

- Página de detalhe de projeto, incluindo tarefas e documentação associada.
- Página de detalhe de tarefa e listas ou páginas de detalhe para colaboradores e programas internos.
- Criação, edição e eliminação de recursos através do frontend.
- Controlo do temporizador através da interface React.
- Interface de alteração e redefinição segura de palavra-passe.
- Testes automatizados de componentes e fluxos do frontend.
- Testes Maven de integração com uma instância PostgreSQL dedicada.
- Revisão da estratégia de armazenamento e renovação do token antes de produção.
- Validação do JAR final e do frontend compilado numa instalação independente.

## Estrutura principal

```text
DevFlow_Hub/
├── backend/
│   └── src/
├── database/
│   ├── migrations/
│   ├── devflow_hub.sql
│   ├── migrate_existing_database.sql
│   └── README.md
├── docs/
│   └── architecture/
├── frontend/
│   ├── public/
│   └── src/
├── scripts/
│   └── smoke-test-api.ps1
├── LOG.md
└── README.md
```

## Base de dados

O DevFlow Hub utiliza PostgreSQL para persistir colaboradores, projetos, tarefas e programas internos.

Os ficheiros principais encontram-se em:

```text
database/
├── migrations/
├── devflow_hub.sql
├── migrate_existing_database.sql
├── INSTALACAO_BASE_DADOS_LOCAL.txt
└── README.md
```

- `devflow_hub.sql` cria uma instalação nova e adiciona dados de demonstração.
- `migrate_existing_database.sql` atualiza bases criadas por versões anteriores.
- `migrations/` contém migrações específicas e datadas.
- `INSTALACAO_BASE_DADOS_LOCAL.txt` explica como preparar uma base local.
- `database/README.md` documenta instalação, migração e validação.

### Dados de demonstração validados

- 6 colaboradores;
- 4 projetos;
- 5 tarefas;
- 4 programas internos;
- foreign keys entre projetos, tarefas, programas e colaboradores.

As instruções completas estão em [`database/INSTALACAO_BASE_DADOS_LOCAL.txt`](database/INSTALACAO_BASE_DADOS_LOCAL.txt).

## Backend

A organização principal do backend segue uma arquitetura por camadas:

```text
backend/src/main/java/com/devflowhub/backend/
├── config/
├── controller/
├── domain/
├── dto/
├── entity/
├── exception/
├── repository/
├── security/
├── service/
└── util/
```

### Responsabilidades

- `config`: configurações técnicas, segurança e JWT;
- `controller`: endpoints REST;
- `domain`: estados, prioridades e valores permitidos;
- `dto`: pedidos e respostas da API;
- `entity`: entidades JPA;
- `exception`: exceções e tratamento global de erros;
- `repository`: acesso aos dados;
- `security`: integração da autenticação JWT com Spring Security;
- `service`: regras de negócio e consultas agregadas;
- `util`: funções comuns de normalização.

## Configuração do backend

### Variáveis de ambiente

```text
DB_URL
DB_USERNAME
DB_PASSWORD
SHOW_SQL
JWT_SECRET
JWT_ISSUER
JWT_EXPIRATION
PORT
```

Exemplo para PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/devflow_hub"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<palavra-passe-local-do-postgresql>"
$env:SHOW_SQL = "false"
$env:JWT_SECRET = "<segredo-de-desenvolvimento-com-comprimento-suficiente>"
$env:JWT_ISSUER = "https://devflow-hub.local"
$env:JWT_EXPIRATION = "PT15M"
$env:PORT = "8080"
```

Credenciais, palavras-passe e segredos não devem ser guardados no Git.

### Compilar e testar

```powershell
Set-Location ".\backend"
.\mvnw.cmd clean test
```

### Iniciar

```powershell
Set-Location ".\backend"
.\mvnw.cmd spring-boot:run
```

A API fica disponível em:

```text
http://localhost:8080/api
```

### Empacotar

```powershell
Set-Location ".\backend"
.\mvnw.cmd clean package -DskipTests
```

O JAR é gerado em:

```text
backend/target/devflow-hub.jar
```

## Frontend

O frontend possui documentação adicional em [`frontend/README.md`](frontend/README.md).

### Pré-requisitos

- Node.js compatível com Vite 8;
- npm;
- backend em execução na porta `8080` para testar a aplicação completa.

### Instalar dependências

```powershell
Set-Location ".\frontend"
npm install
```

### Configuração da API

O ficheiro de exemplo contém:

```text
VITE_API_BASE_URL=
```

Em desenvolvimento local, o valor pode permanecer vazio. O servidor Vite encaminha os pedidos iniciados por `/api` para `http://localhost:8080`.

Para um backend alojado noutra origem, cria `frontend/.env.local` e define, por exemplo:

```text
VITE_API_BASE_URL=https://api.exemplo.com
```

O ficheiro `.env.local` é local e não deve ser enviado para o repositório.

### Iniciar o frontend

```powershell
Set-Location ".\frontend"
npm run dev
```

Por predefinição, a aplicação fica disponível em:

```text
http://localhost:5173
```

### Validar o frontend

```powershell
npm run lint
npm run build
```

O build de produção é gerado em `frontend/dist/`, que permanece fora do Git.

## API REST

### Autenticação

```text
POST /api/auth/login
PUT  /api/auth/change-password
```

O login é público. A alteração de palavra-passe requer autenticação.

Exemplo de pedido:

```json
{
  "email": "<email-do-colaborador>",
  "password": "<palavra-passe-do-colaborador>"
}
```

Exemplo abreviado de resposta:

```json
{
  "accessToken": "<jwt-token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "collaborator": {
    "id": 1,
    "name": "<nome-do-colaborador>",
    "email": "<email-do-colaborador>",
    "role": "<função>",
    "active": true
  }
}
```

Os pedidos protegidos devem incluir:

```http
Authorization: Bearer <jwt-token>
```

### Colaboradores

```text
GET    /api/collaborators
GET    /api/collaborators/{id}
POST   /api/collaborators
PUT    /api/collaborators/{id}
DELETE /api/collaborators/{id}
```

### Projetos

```text
GET    /api/projects
GET    /api/projects/{id}
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

### Tarefas

```text
GET    /api/tasks
GET    /api/tasks/{id}
POST   /api/tasks
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
POST   /api/tasks/{id}/start-timer
POST   /api/tasks/{id}/pause-timer
POST   /api/tasks/{id}/resume-timer
GET    /api/tasks/{id}/total-time
GET    /api/tasks/{id}/timer
POST   /api/tasks/{id}/complete
```

### Programas internos

```text
GET    /api/internal-programs
GET    /api/internal-programs/{id}
POST   /api/internal-programs
PUT    /api/internal-programs/{id}
DELETE /api/internal-programs/{id}
```

### Dashboard

```text
GET /api/dashboard
```

A resposta inclui:

- totais de colaboradores, projetos, tarefas e programas;
- contagem de tarefas por estado;
- tempo total registado;
- tarefas recentes;
- projetos com prazos futuros.

## Detalhe de projetos no frontend

O frontend disponibiliza uma página protegida de detalhe através da rota:

```text
/projects/:projectId
```

A página:

- obtém o projeto através de `GET /api/projects/{id}`;
- resolve o nome do gestor com os dados de `GET /api/collaborators`;
- carrega as tarefas através de `GET /api/tasks`;
- filtra apenas as tarefas cujo `projectId` corresponde ao projeto;
- apresenta estado, descrição, gestor, datas e número de tarefas;
- apresenta responsável, prioridade, estado, tempo registado e última atualização das tarefas associadas;
- trata projetos sem tarefas;
- trata identificadores inválidos e projetos inexistentes;
- mantém a sessão e a navegação autenticadas;
- possui layout responsivo para desktop e dispositivos móveis.

A filtragem das tarefas é atualmente realizada no frontend porque a API ainda não possui um endpoint específico como `GET /api/projects/{id}/tasks`.

## Detalhe de tarefas e temporizador no frontend

O frontend disponibiliza uma página protegida através da rota:

```text
/tasks/:taskId
```

A página:

- obtém a tarefa através de `GET /api/tasks/{id}`;
- resolve o projeto através de `GET /api/projects`;
- resolve o responsável através de `GET /api/collaborators`;
- apresenta estado, prioridade, descrição e relações;
- apresenta o tempo acumulado e o estado do temporizador;
- atualiza visualmente o tempo enquanto o temporizador está ativo;
- liga o projeto associado ao respetivo detalhe;
- trata tarefas sem projeto ou responsável;
- trata identificadores inválidos e tarefas inexistentes;
- mantém a sessão e a navegação autenticadas;
- possui layout responsivo para desktop e dispositivos móveis.

A página também permite executar:

```text
POST /api/tasks/{id}/start-timer
POST /api/tasks/{id}/pause-timer
POST /api/tasks/{id}/resume-timer
POST /api/tasks/{id}/complete
```

Durante estas operações:

- os botões ficam temporariamente desativados;
- são apresentados estados de processamento;
- são apresentadas mensagens de sucesso ou erro;
- a conclusão exige confirmação;
- o tempo da sessão ativa é acumulado ao concluir;
- uma tarefa concluída não pode reiniciar o temporizador.
## Segurança e sessão do frontend

A implementação atual guarda a sessão JWT em `sessionStorage` e calcula localmente a data de expiração com base no campo `expiresIn`.

A sessão é eliminada quando:

- o utilizador termina a sessão;
- o tempo de validade termina;
- uma chamada autenticada devolve HTTP `401`;
- os dados armazenados são inválidos.

Esta solução é adequada para a fase académica atual. Antes de uma utilização de produção, deve ser revista a estratégia de armazenamento do token, mitigação de XSS, renovação de sessão e eventual utilização de cookies `HttpOnly`, `Secure` e `SameSite`.

## Tratamento de erros

A API possui tratamento estruturado para:

- recursos não encontrados;
- operações inválidas;
- credenciais inválidas;
- pedidos não autenticados;
- erros de validação;
- JSON malformado;
- erros inesperados da aplicação.

As respostas não expõem stack traces, hashes de palavras-passe nem detalhes internos.

## Testes automatizados

### Backend

```powershell
Set-Location ".\backend"
.\mvnw.cmd clean test
```

Na validação realizada em 23/07/2026:

```text
Tests run: 38
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Frontend

```powershell
Set-Location ".\frontend"
npm run lint
npm run build
```

Na validação realizada em 24/07/2026, ambos os comandos terminaram com sucesso.

## Smoke tests da API com PostgreSQL

O script encontra-se em:

```text
scripts/smoke-test-api.ps1
```

Com o backend em execução:

```powershell
powershell.exe `
    -NoProfile `
    -ExecutionPolicy Bypass `
    -File ".\scripts\smoke-test-api.ps1"
```

O script valida autenticação JWT, proteção dos endpoints, CRUD, relações, regras de negócio, temporizador, campos de auditoria, tratamento de erros e limpeza dos dados temporários.

Uma execução bem-sucedida termina com:

```text
All PostgreSQL API smoke tests passed.
```

Outro endereço pode ser fornecido com:

```powershell
powershell.exe `
    -NoProfile `
    -ExecutionPolicy Bypass `
    -File ".\scripts\smoke-test-api.ps1" `
    -BaseUrl "http://localhost:8080"
```

## Estratégia de branches

- `main`: versões estáveis e prontas para entrega;
- `develop`: integração das funcionalidades;
- `feature/*`: novas funcionalidades;
- `fix/*`: correções;
- `refactor/*`: melhorias estruturais;
- `test/*`: testes;
- `docs/*`: documentação;
- `chore/*`: configuração, automação e manutenção.

## Autora

Daniela Torres Almeida
