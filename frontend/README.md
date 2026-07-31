# DevFlow Hub Frontend

Frontend React do DevFlow Hub, responsável pela autenticação do utilizador, navegação protegida e apresentação dos dados fornecidos pela API Spring Boot.

## Tecnologias

- React 19.2.8 (versão bloqueada em `package-lock.json`)
- TypeScript 6.0.3 (versão bloqueada em `package-lock.json`)
- Vite 8.1.5 (versão bloqueada em `package-lock.json`)
- React Router 8.3.0
- Fetch API nativa
- ESLint
- Vitest
- React Testing Library
- jsdom

## Funcionalidades atuais

- página de login ligada a `POST /api/auth/login`;
- sessão JWT guardada em `sessionStorage`;
- rotas `/dashboard`, `/projects`, `/projects/:projectId`, `/tasks` e `/tasks/:taskId` protegidas;
- persistência da sessão após atualização da página;
- logout manual e expiração automática;
- inclusão automática do Bearer token nos pedidos autenticados;
- tratamento global de HTTP `401`;
- dashboard ligado a `GET /api/dashboard`;
- indicadores, tarefas recentes e projetos com prazos próximos;
- contagem de programas internos apresentada no dashboard, com indicação de que a interface de gestão está planeada;
- cabeçalho autenticado reutilizável;
- lista e detalhe de projetos;
- criação, edição e eliminação de projetos;
- criador registado automaticamente como `OWNER` e gestor inicial;
- edição de projetos disponível a `OWNER` e `MANAGER`;
- eliminação de projetos reservada ao `OWNER`;
- gestão de memberships com adição, alteração de papel e remoção lógica;
- transferência explícita de ownership;
- lista e detalhe de tarefas;
- criação de tarefas independentes e associadas a projetos;
- edição de título, descrição, estado, prioridade, projeto e responsável;
- eliminação de tarefas com confirmação;
- responsáveis de projeto limitados às memberships ativas elegíveis;
- `CONTRIBUTOR` limitado à atribuição a si próprio;
- `VIEWER` em modo de consulta;
- início, pausa, retoma e conclusão do temporizador;
- atualização visual do tempo durante uma sessão ativa;
- mensagens de sucesso, validação, conflito e erro;
- estados de loading, retry, recurso inexistente e ausência de dados;
- layout responsivo;
- testes automatizados dos clientes API, formulários, autenticação, permissões, componentes e rotas.

Os controlos apresentados no frontend refletem as permissões conhecidas pela interface, mas não substituem a autorização do backend. A API continua a decidir se o utilizador pode ver ou alterar cada projeto ou tarefa.

## Estrutura principal

```text
frontend/src/
├── api/
│   ├── apiClient.ts
│   ├── authApi.ts
│   ├── collaboratorsApi.ts
│   ├── dashboardApi.ts
│   ├── projectMembershipsApi.ts
│   ├── projectOwnershipApi.ts
│   ├── projectsApi.ts
│   ├── tasksApi.ts
│   └── *.test.ts
├── auth/
├── components/
│   ├── AppHeader.tsx
│   ├── ProjectForm.tsx
│   ├── ProjectMembersPanel.tsx
│   ├── ProjectOwnershipTransferPanel.tsx
│   ├── TaskForm.tsx
│   └── *.test.tsx
├── pages/
├── routes/
├── test/
├── types/
├── utils/
├── index.css
└── main.tsx
```

## Pré-requisitos

- Node.js `>=22.22.0`;
- npm;
- backend DevFlow Hub em execução para testar a aplicação completa.

## Instalação

```powershell
Set-Location ".\frontend"
npm install
```

Para uma instalação reproduzível baseada no `package-lock.json`:

```powershell
npm ci
```

## Variáveis de ambiente

Copia o ficheiro de exemplo apenas quando for necessário configurar outra origem:

```powershell
Copy-Item ".env.example" ".env.local"
```

Valor disponível:

```text
VITE_API_BASE_URL=
```

### Desenvolvimento local

Mantém `VITE_API_BASE_URL` vazio. O proxy do Vite encaminha `/api` para:

```text
http://localhost:8080
```

### Backend noutra origem

Define a origem sem uma barra final desnecessária:

```text
VITE_API_BASE_URL=https://api.exemplo.com
```

O ficheiro `.env.local` não deve ser enviado para o Git nem incluído numa entrega partilhada.

## Executar

```powershell
npm run dev
```

A aplicação fica normalmente disponível em:

```text
http://localhost:5173
```

## Validação

```powershell
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

O build de produção é criado em `dist/`. A pasta `dist`, tal como `node_modules`, permanece fora do Git.

## Rotas atuais

```text
/login                  pública
/dashboard              protegida
/projects               protegida
/projects/:projectId    protegida
/tasks                  protegida
/tasks/:taskId          protegida
*                       página não encontrada
```

A rota `/` redireciona para `/dashboard`. Quando não existe uma sessão válida, o utilizador é encaminhado para `/login`.

## Autenticação

O login devolve:

- `accessToken`;
- `tokenType`;
- `expiresIn`;
- dados públicos do colaborador.

O frontend converte `expiresIn` numa data absoluta `expiresAt` e guarda a sessão em `sessionStorage`.

Uma resposta HTTP `401` num pedido autenticado emite um evento interno, limpa a sessão e faz com que a rota protegida redirecione para o login.

## Segurança

A solução atual com `sessionStorage` é adequada à fase académica do projeto, mas continua acessível a JavaScript. Antes de produção, devem ser avaliadas medidas adicionais contra XSS, renovação do token e uma possível estratégia com cookies `HttpOnly`, `Secure` e `SameSite`.

Os controlos apresentados no frontend não substituem a autorização do backend. A API continua a decidir se o utilizador pode ver ou alterar cada projeto ou tarefa.

## Scripts npm

```text
npm run dev         inicia o servidor Vite
npm run lint        executa o ESLint
npm test            executa a suite Vitest uma vez
npm run test:watch  executa o Vitest em modo watch
npm run build       valida TypeScript e gera o build
npm run preview     pré-visualiza o build localmente
```
