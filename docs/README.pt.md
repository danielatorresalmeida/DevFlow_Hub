# DevFlow Hub

DevFlow Hub é uma aplicação web académica para gestão de colaboradores, projetos, tarefas, tempo de trabalho, documentos e programas internos.

## Estado do repositório

Este repositório representa uma reorganização limpa e estruturada do projeto DevFlow Hub. O código foi desenvolvido anteriormente num repositório de trabalho e foi reorganizado para:

- remover ficheiros gerados, dependências instaladas e configurações locais;
- separar funcionalidades por branches e commits coerentes;
- manter o README e o LOG alinhados com o código;
- preparar uma entrega portátil com backend, frontend, SQL e documentação;
- preservar um histórico técnico claro para avaliação académica.

As datas dos commits reorganizados representam a organização técnica do novo repositório e não substituem as datas reais registadas no `LOG.md`.

O estado descrito neste README corresponde à versão preparada para a apresentação final de 30/07/2026, incluindo a gestão de projetos e tarefas no frontend.

## Arquitetura atual

```text
React + TypeScript + Vite
          |
          | HTTP, JSON e JWT Bearer
          v
Spring Boot REST API
          |
          +------------------+
          |                  |
          v                  v
     PostgreSQL       Object storage local
```

O frontend React é responsável pela interface, navegação, autenticação no cliente e apresentação dos dados. O backend Spring Boot é responsável pela segurança, autorização, regras de negócio, validação, persistência, metadados documentais e respostas da API.

O armazenamento de objetos está preparado no backend, mas a API HTTP atual expõe apenas os metadados dos anexos. O upload, download e eliminação do conteúdo permanecem no roadmap.

A decisão arquitetural do frontend está documentada em [`docs/architecture/frontend-decision.md`](docs/architecture/frontend-decision.md).

A arquitetura de controlo de acesso está documentada em:

- [`docs/architecture/project-access-control.md`](docs/architecture/project-access-control.md);
- [`docs/architecture/project-access-permission-matrix.md`](docs/architecture/project-access-permission-matrix.md);
- [`docs/architecture/project-access-endpoint-inventory.md`](docs/architecture/project-access-endpoint-inventory.md).

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

- React 19.2.8 (versão bloqueada em `package-lock.json`)
- TypeScript 6.0.3 (versão bloqueada em `package-lock.json`)
- Vite 8.1.5 (versão bloqueada em `package-lock.json`)
- React Router 8.3.0
- Fetch API nativa
- ESLint
- Vitest
- React Testing Library
- jsdom

### Base de dados e armazenamento

- PostgreSQL
- H2 em memória para testes automatizados
- Object storage local configurável através de uma abstração própria

### Qualidade e automação

- JUnit 5
- Mockito
- MockMvc
- Spring Boot Test
- Smoke tests em PowerShell com PostgreSQL real
- GitHub Actions para backend e frontend
- ESLint, Vitest e build TypeScript/Vite no frontend

## Estado atual

### Backend e API

- Estrutura PostgreSQL criada e validada numa base limpa.
- Scripts de instalação e migração disponíveis.
- Entidades JPA, repositories e services implementados.
- CRUD de colaboradores, projetos, tarefas, documentos e programas internos implementado.
- Consulta de metadata de anexos implementada.
- Temporizador de tarefas com início, pausa, retoma, conclusão e acumulação de tempo.
- Campos de auditoria das tarefas protegidos e geridos automaticamente.
- Endpoint agregado `GET /api/dashboard` implementado.
- Tratamento global de erros estruturados para HTTP `400`, `401`, `403`, `404` e conflitos de integridade `409`.
- JSON malformado rejeitado sem expor stack traces ou detalhes internos.
- Autenticação JWT implementada.
- Login público em `POST /api/auth/login`.
- Restantes endpoints em `/api/**` protegidos com Bearer token.
- Tokens com expiração predefinida de 15 minutos.
- Colaboradores inativos impedidos de iniciar sessão.
- Alteração de palavra-passe associada ao colaborador autenticado através da claim `sub`.
- Resolução central do colaborador autenticado através de `CurrentCollaboratorResolver`.
- Persistência de memberships de projeto com os papéis `OWNER`, `MANAGER`, `CONTRIBUTOR` e `VIEWER`.
- Apenas memberships `ACTIVE` concedem acesso ao projeto.
- Listagem, consulta, atualização e eliminação de projetos protegidas por autorização de recurso.
- Criação de projeto transacional com membership `OWNER` automática para o criador.
- Listagem e consulta de tarefas filtradas pelo colaborador autenticado.
- Tarefas independentes limitadas ao respetivo assignee.
- Operações do temporizador limitadas ao assignee da tarefa.
- Movimentação de tarefas valida o projeto atual, o projeto de destino e o assignee antes de persistir alterações.
- Recursos inexistentes ou ocultos devolvem `404`; ações conhecidas mas não permitidas devolvem `403`.
- API de gestão de memberships com listagem, adição, alteração de papel e remoção lógica de membros.
- `OWNER` gere `MANAGER`, `CONTRIBUTOR` e `VIEWER`; `MANAGER` gere apenas `CONTRIBUTOR` e `VIEWER`.
- Endpoints genéricos de memberships não criam, alteram nem removem `OWNER`.
- Memberships removidas passam a `INACTIVE` e podem ser reativadas sem criar registos duplicados.
- `project.managerId` é validado contra colaboradores ativos com membership ativa e papel elegível.
- Transferência explícita e transacional de ownership com atualização coordenada das duas memberships e de `project.managerId`.
- Duplicados, versões desatualizadas e conflitos de concorrência devolvem `409 Conflict`.
- Fundação de documentos e attachments persistida na base de dados.
- Object storage local com proteção contra caminhos inseguros e symlinks.
- Provider e diretório do object storage configuráveis em runtime.
- Backend validado com PostgreSQL real e `ddl-auto=validate`.
- Suite backend validada em 29/07/2026 com 236 testes sem falhas.

### Frontend React

- Interface dedicada de transferência de ownership disponível apenas ao `OWNER`, com confirmação explícita, controlo de concorrência e atualização imediata do gestor e das memberships.
- Fundação React, TypeScript e Vite implementada.
- Rotas `/login`, `/dashboard`, `/projects`, `/projects/:projectId`, `/tasks`, `/tasks/:taskId` e página de recurso não encontrado.
- Rotas autenticadas protegidas.
- Integração real com `POST /api/auth/login`.
- Sessão autenticada guardada em `sessionStorage`.
- Persistência da sessão após atualização da página.
- Logout manual e expiração automática da sessão.
- Cabeçalho `Authorization: Bearer <token>` aplicado aos pedidos autenticados.
- Tratamento global de respostas `401`, limpeza da sessão e redirecionamento para `/login`.
- Dashboard autenticado ligado a `GET /api/dashboard`.
- Indicadores, distribuição de tarefas, tarefas recentes e projetos com prazos próximos.
- A contagem de programas internos é apresentada no dashboard; a interface de gestão permanece planeada.
- Cabeçalho autenticado reutilizável com identidade do utilizador, navegação e logout.
- Lista autenticada de projetos ligada a `GET /api/projects`.
- Página de detalhe de projeto ligada a `GET /api/projects/{id}`.
- Criação, edição e eliminação de projetos através da interface.
- O criador torna-se automaticamente `OWNER` e gestor inicial.
- Edição disponível a `OWNER` e `MANAGER`; eliminação reservada ao `OWNER`.
- Apresentação das tarefas associadas ao projeto.
- Painel de membros integrado na página de detalhe do projeto, com listagem, adição, alteração de papel e remoção lógica.
- Ações de gestão de membros adaptadas ao papel da membership do utilizador autenticado.
- Mensagens de conflito de memberships sem instruções duplicadas e cursor `not-allowed` nos botões de ação desativados.
- Lista autenticada de tarefas ligada a `GET /api/tasks`.
- Página de detalhe de tarefa ligada a `GET /api/tasks/{id}`.
- Criação, edição e eliminação de tarefas através da interface.
- Alteração de título, descrição, estado, prioridade, projeto e responsável.
- Tarefas independentes atribuídas ao colaborador autenticado.
- Responsáveis de projeto limitados às memberships ativas elegíveis.
- Apresentação do projeto, responsável, estado, prioridade, datas de criação e última atualização e tempo registado.
- Início, pausa, retoma e conclusão do temporizador através da interface.
- Atualização visual do tempo durante uma sessão ativa.
- Estados de carregamento, erro, retry, recurso inexistente e ausência de dados.
- Interface responsiva validada em desktop e numa viewport móvel de `390 × 844`.
- Testes automatizados de armazenamento da autenticação, rotas protegidas e configuração de rotas.
- `npm run lint`, `npm test` e `npm run build` validados com sucesso.
- Suite frontend validada em 29/07/2026 com 53 testes em 12 ficheiros.

### Alinhamento com o planeamento inicial

A versão preparada para apresentação concretiza de ponta a ponta os requisitos funcionais RF01 a RF15 e RF18 definidos no relatório inicial:

- autenticação de colaboradores;
- dashboard com dados dinâmicos;
- consulta, criação, edição e eliminação de projetos;
- consulta, criação, edição e eliminação de tarefas;
- alteração de estado, prioridade, projeto e responsável;
- associação entre tarefas e projetos;
- temporizador com início, pausa, retoma, conclusão e tempo acumulado;
- API REST e persistência PostgreSQL.

Os requisitos RF16 e RF17 possuem CRUD persistente e API REST no backend, mas continuam parcialmente concluídos por ainda não existir uma página React dedicada aos programas internos.

A arquitetura, organização por camadas, execução local, documentação e automação de testes concretizam os requisitos não funcionais dentro do âmbito académico. A comparação detalhada entre a proposta inicial e o resultado final será apresentada no relatório final.

### Trabalho ainda pendente e roadmap

- Página React para consulta e gestão de programas internos.
- Autorização de documentos e anexos através da mesma cadeia de acesso dos projetos e tarefas.
- Páginas de notas associadas a projetos e tarefas.
- Proveniência de documentos e anexos, incluindo `createdById` e `uploadedById`.
- Upload, download e eliminação segura de conteúdo de anexos através da API.
- Centro de importação para migrar projetos, tarefas, notas, colaboradores e registos de tempo do Notion e do Toggl Track.
- Mapeamento de campos, prevenção de duplicados, resolução de conflitos e rastreabilidade das importações.
- Interface de alteração e redefinição segura de palavra-passe.
- Política administrativa global separada para colaboradores e programas internos.
- Ajuste do dashboard para distinguir métricas pessoais de métricas globais.
- Testes Maven de integração com uma instância PostgreSQL dedicada.
- Revisão da estratégia de armazenamento e renovação do token antes de produção.
- Renovação segura da sessão com access token curto, refresh token, aviso de expiração e proteção contra perda de dados não guardados.
- Datas próprias de planeamento das tarefas, com `startDate`, `dueDate`, validação cronológica e indicadores de atraso.
- Internacionalização centralizada da interface, começando por inglês e português, sem tradução automática do conteúdo introduzido pelos utilizadores na primeira fase.
- Ocultação do identificador interno ou apresentação de um código funcional como `PRJ-0025`, mantendo os IDs persistentes da base de dados.
- Validação dos artefactos finais numa instalação independente.

## Estrutura principal

```text
DevFlow_Hub/
├── .github/
│   └── workflows/
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

As pastas geradas `backend/target`, `frontend/node_modules` e `frontend/dist`, assim como ficheiros locais `.env.local`, permanecem fora do Git e não devem ser incluídas numa entrega limpa.

## Base de dados

O DevFlow Hub utiliza PostgreSQL para persistir:

- colaboradores;
- projetos;
- memberships de projeto;
- tarefas;
- documentos;
- metadata de anexos;
- programas internos.

Os números apresentados como `PROJECT #...` e `TASK #...` são identificadores internos persistentes. Uma eliminação não renumera os registos nem reutiliza automaticamente IDs antigos, pelo que podem existir intervalos. As sequências não devem ser reiniciadas numa base de produção.

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

### Dados de demonstração de uma instalação nova

O script `database/devflow_hub.sql` cria uma base local reproduzível com:

- 3 colaboradores;
- 2 projetos;
- 3 tarefas;
- 2 programas internos;
- memberships derivadas dos gestores e responsáveis das tarefas;
- foreign keys entre projetos, tarefas, programas e colaboradores;
- constraints e índices para memberships, documentos e attachments.

A base utilizada durante a apresentação pode conter mais registos criados manualmente. Esses dados locais não fazem parte automaticamente de uma instalação nova.

#### Contas de teste da base preparada para a apresentação

Para validar a matriz completa de permissões na base local preparada para a apresentação, estão configuradas as seguintes contas:

| Papel de projeto | Nome | Email |
|---|---|---|
| `OWNER` | Bruno Silva | `bruno.silva@devflowhub.pt` |
| `MANAGER` | Daniel Rocha | `daniel.rocha@devflowhub.pt` |
| `CONTRIBUTOR` | Carla Gomes | `carla.gomes@devflowhub.pt` |
| `VIEWER` | Ana Silva | `ana.silva@devflowhub.pt` |

Todas usam a palavra-passe local de demonstração:

```text
DevFlowTest-123!
```

Estas contas permitem testar diretamente os quatro papéis sem alterar memberships durante a demonstração. A coluna profissional `Collaborator.role` continua a ser informativa; as permissões efetivas são determinadas pelas memberships ativas de cada projeto.

> Estas são credenciais públicas de demonstração destinadas exclusivamente ao ambiente académico local. Devem ser alteradas ou removidas antes de qualquer utilização fora desse ambiente.

A documentação destas contas não cria automaticamente os utilizadores. Para que funcionem numa instalação nova, os mesmos colaboradores, o hash da palavra-passe e as memberships correspondentes devem existir em `database/devflow_hub.sql` ou ser adicionados por uma migração de dados equivalente. Enquanto o seed não for sincronizado, os dados mínimos criados pelo script podem ser diferentes dos dados da base preparada para a apresentação.

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
├── storage/
└── util/
```

### Responsabilidades

- `config`: configurações técnicas, segurança, JWT e object storage;
- `controller`: endpoints REST;
- `domain`: estados, prioridades, papéis e valores permitidos;
- `dto`: pedidos e respostas da API;
- `entity`: entidades JPA;
- `exception`: exceções e tratamento global de erros;
- `repository`: acesso aos dados e queries filtradas por autorização;
- `security`: autenticação JWT e autorização de recursos;
- `service`: regras de negócio e consultas agregadas;
- `storage`: contratos e providers de armazenamento de objetos;
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
OBJECT_STORAGE_PROVIDER
OBJECT_STORAGE_LOCAL_ROOT_DIRECTORY
PORT
```

Exemplo para PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/devflow_hub"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<palavra-passe-local-do-postgresql>"
$env:SHOW_SQL = "false"
$env:JWT_SECRET = "<segredo-base64-com-pelo-menos-32-bytes-depois-da-descodificacao>"
$env:JWT_ISSUER = "https://devflow-hub.local"
$env:JWT_EXPIRATION = "PT15M"
$env:OBJECT_STORAGE_PROVIDER = "local"
$env:OBJECT_STORAGE_LOCAL_ROOT_DIRECTORY = ".\data\object-storage"
$env:PORT = "8080"
```

`JWT_SECRET` deve ser Base64 válido e conter pelo menos 32 bytes depois da descodificação.

O valor predefinido de `JWT_EXPIRATION` é `PT15M`. A implementação atual não possui refresh token, pelo que a sessão termina aproximadamente 15 minutos depois do login mesmo quando existe atividade. Durante testes manuais prolongados pode ser usado outro valor local, por exemplo `PT1H`, sem alterar o código nem guardar essa configuração no Git.

Credenciais reais, palavras-passe privadas, segredos e configurações locais não devem ser guardados no Git. A única exceção são as credenciais públicas de demonstração documentadas acima, criadas exclusivamente para o ambiente académico local.

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

- Node.js `>=22.22.0`;
- npm;
- backend em execução na porta `8080` para testar a aplicação completa.

### Instalar dependências

```powershell
Set-Location ".\frontend"
npm install
```

Em CI e em instalações reproduzíveis com `package-lock.json`, utilizar:

```powershell
npm ci
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

O ficheiro `.env.local` é local e não deve ser enviado para o repositório nem incluído numa entrega partilhada.

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
npm test
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

Estes endpoints estão autenticados, mas ainda necessitam de uma política administrativa global separada dos papéis de projeto.

### Projetos

```text
GET    /api/projects
GET    /api/projects/{id}
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}
GET    /api/projects/{projectId}/members
POST   /api/projects/{projectId}/members
PATCH  /api/projects/{projectId}/members/{collaboratorId}
DELETE /api/projects/{projectId}/members/{collaboratorId}
POST   /api/projects/{projectId}/ownership-transfer
```

Regras principais:

- `GET /api/projects` devolve apenas projetos com membership ativa do utilizador;
- qualquer membro ativo pode consultar um projeto;
- `OWNER` e `MANAGER` podem atualizar o projeto;
- apenas `OWNER` pode eliminar o projeto;
- o criador torna-se `OWNER` e manager inicial numa única transação;
- todos os membros ativos podem consultar a lista de memberships;
- `OWNER` pode gerir `MANAGER`, `CONTRIBUTOR` e `VIEWER`;
- `MANAGER` pode gerir apenas `CONTRIBUTOR` e `VIEWER`;
- `CONTRIBUTOR` e `VIEWER` não podem gerir memberships;
- a remoção é lógica através do estado `INACTIVE`;
- adicionar novamente um antigo membro reativa a membership existente;
- apenas o `OWNER` atual pode transferir ownership;
- o destinatário deve ser colaborador ativo com membership `ACTIVE` no projeto;
- o novo proprietário passa a `OWNER`, o anterior passa a `MANAGER` e `project.managerId` é atualizado na mesma transação;
- versões desatualizadas e conflitos de concorrência devolvem `409 Conflict`.

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

Regras principais:

- as listagens são filtradas no backend;
- tarefas de projeto seguem a membership e o papel do utilizador;
- tarefas sem projeto são privadas do assignee;
- ações do temporizador pertencem ao assignee;
- alterações de projeto validam o parent atual e o parent de destino antes do save;
- um assignee de tarefa de projeto deve ser membro ativo do projeto.

### Documentos

```text
GET    /api/documents/{id}
GET    /api/projects/{projectId}/documents
GET    /api/tasks/{taskId}/documents
POST   /api/documents
PUT    /api/documents/{id}
DELETE /api/documents/{id}
```

Os endpoints e a persistência estão implementados. A autorização específica de documentos e a proveniência do criador continuam pendentes.

### Anexos

```text
GET /api/attachments/{id}
GET /api/documents/{documentId}/attachments
```

A API atual expõe apenas metadata. Upload, download e eliminação de conteúdo ainda não estão disponíveis por HTTP.

### Programas internos

```text
GET    /api/internal-programs
GET    /api/internal-programs/{id}
POST   /api/internal-programs
PUT    /api/internal-programs/{id}
DELETE /api/internal-programs/{id}
```

Tal como os colaboradores, os programas internos ainda necessitam de uma política administrativa global própria.

### Dashboard

```text
GET /api/dashboard
```

A resposta inclui:

- total de colaboradores;
- projetos acessíveis ao utilizador;
- tarefas acessíveis ao utilizador;
- total de programas internos;
- contagem das tarefas acessíveis por estado;
- tempo total registado nas tarefas acessíveis;
- tarefas recentes acessíveis;
- projetos acessíveis com prazos futuros.

Os valores de colaboradores e programas continuam globais. Esta diferença deve ser resolvida quando for introduzida a autorização administrativa global.

## Detalhe de projetos no frontend

O frontend disponibiliza uma página protegida de detalhe através da rota:

```text
/projects/:projectId
```

A página:

- obtém o projeto através de `GET /api/projects/{id}`;
- permite criar projetos a partir da página de listagem;
- permite editar nome, descrição, estado, datas e gestor;
- permite eliminar o projeto com confirmação quando o utilizador é `OWNER`;
- calcula os controlos disponíveis a partir da membership ativa;
- resolve o nome do gestor com os dados de `GET /api/collaborators`;
- carrega as tarefas através de `GET /api/tasks`;
- filtra apenas as tarefas cujo `projectId` corresponde ao projeto;
- apresenta estado, descrição, gestor, datas e número de tarefas;
- apresenta responsável, prioridade, estado, tempo registado e última atualização das tarefas associadas;
- trata projetos sem tarefas;
- trata identificadores inválidos e projetos inexistentes;
- mantém a sessão e a navegação autenticadas;
- possui layout responsivo para desktop e dispositivos móveis.

A filtragem das tarefas associadas é realizada no frontend sobre uma lista que já foi filtrada pelo backend de acordo com o acesso do utilizador.

## Detalhe de tarefas e temporizador no frontend

O frontend disponibiliza uma página protegida através da rota:

```text
/tasks/:taskId
```

A página:

- obtém a tarefa através de `GET /api/tasks/{id}`;
- permite criar tarefas independentes e associadas a projetos;
- permite editar título, descrição, estado, prioridade, projeto e responsável;
- permite eliminar tarefas com confirmação;
- adapta responsáveis e operações às permissões do utilizador;
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
- são apresentadas mensagens de sucesso e erro;
- a conclusão exige confirmação;
- o tempo da sessão ativa é acumulado ao concluir;
- uma tarefa concluída não pode reiniciar o temporizador.

O backend pode recusar uma ação apresentada pela interface quando o utilizador não é o assignee. Uma melhoria futura deve ocultar ou desativar estas ações através de capabilities devolvidas pela API.

## Segurança e autorização

### Sessão do frontend

A implementação atual guarda a sessão JWT em `sessionStorage` e calcula localmente a data de expiração com base no campo `expiresIn`.

A sessão é eliminada quando:

- o utilizador termina a sessão;
- o tempo de validade termina;
- uma chamada autenticada devolve HTTP `401`;
- os dados armazenados são inválidos.

Com a configuração predefinida `PT15M`, a expiração é absoluta e não é renovada pela atividade do utilizador. Esta limitação deve ser considerada durante demonstrações e testes manuais longos.

Esta solução é adequada para a fase académica atual. Antes de uma utilização de produção, deve ser revista a estratégia de armazenamento do token, mitigação de XSS e renovação segura da sessão. A evolução recomendada inclui access token curto, refresh token seguro, aviso antes da expiração, logout após inatividade real e proteção contra perda de dados não guardados, considerando também cookies `HttpOnly`, `Secure` e `SameSite`.

### Autorização de projetos e tarefas

A autenticação identifica o colaborador. A autorização determina se esse colaborador pode aceder a um recurso específico.

A membership ativa do projeto é a fonte de verdade. O campo profissional `Collaborator.role` não é utilizado como papel de autorização do projeto.

A matriz atual é resumida da seguinte forma:

| Papel | Ver projeto | Contribuir | Gerir projeto | Eliminar projeto |
|---|---:|---:|---:|---:|
| `OWNER` | Sim | Sim | Sim | Sim |
| `MANAGER` | Sim | Sim | Sim | Não |
| `CONTRIBUTOR` | Sim | Sim | Não | Não |
| `VIEWER` | Sim | Não | Não | Não |

As regras mais específicas das tarefas encontram-se centralizadas em `TaskAccessService`.

## Tratamento de erros

A API possui tratamento estruturado para:

- `400 Bad Request`: validação, JSON inválido e operações incompatíveis com as regras de negócio;
- `401 Unauthorized`: token ausente ou inválido, colaborador inexistente ou inativo;
- `403 Forbidden`: recurso conhecido, mas operação não permitida pelo papel ativo;
- `404 Not Found`: recurso inexistente ou oculto para evitar divulgação da sua existência;
- `409 Conflict`: registo duplicado, versão desatualizada ou conflito de concorrência;
- erros inesperados da aplicação sem exposição de detalhes internos.

As respostas não expõem stack traces, hashes de palavras-passe nem detalhes internos.

## Testes automatizados

### Backend

```powershell
Set-Location ".\backend"
.\mvnw.cmd clean test
```

Na validação realizada em 29/07/2026:

```text
Tests run: 236
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

A suite inclui testes de contexto, configuração, controllers, serviços, repositories, persistência, autenticação, autorização de projetos, autorização de tarefas, movimentação de tarefas entre projetos, gestão de memberships, transferência de ownership, concorrência otimista, object storage e regras de domínio.

### Frontend

```powershell
Set-Location ".\frontend"
npm run lint
npm test
npm run build
```

Na validação realizada em 29/07/2026:

```text
Test Files: 12 passed
Tests: 53 passed
Lint: aprovado
Build: aprovado
```

## Validação automática no GitHub

O workflow encontra-se em:

```text
.github/workflows/build-validation.yml
```

É executado em pull requests para `develop`, pushes para `develop` e manualmente através de `workflow_dispatch`.

O job do backend executa:

```text
./mvnw --batch-mode clean verify
```

O job do frontend executa:

```text
npm ci
npm run lint
npm run test
npm run build
```

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
