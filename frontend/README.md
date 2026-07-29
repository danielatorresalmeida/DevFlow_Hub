# DevFlow Hub Frontend

Frontend React do DevFlow Hub, responsável pela autenticação do utilizador, navegação protegida e apresentação dos dados fornecidos pela API Spring Boot.

## Tecnologias

- React 19
- TypeScript 6
- Vite 8
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
- indicadores, tarefas recentes e projetos próximos;
- estados de loading, erro, retry e ausência de dados;
- cabeçalho autenticado reutilizável;
- navegação entre dashboard, projetos e tarefas;
- lista autenticada de projetos;
- associação dos projetos aos respetivos gestores;
- apresentação de estado, descrição, gestor e datas;
- estados de loading, erro, retry e lista vazia nos projetos;
- ligação de cada cartão para o respetivo detalhe;
- detalhe do projeto ligado a `GET /api/projects/{id}`;
- apresentação das tarefas associadas ao projeto;
- resolução dos nomes do gestor e dos responsáveis;
- estados de projeto inexistente, identificador inválido e projeto sem tarefas;
- lista autenticada de tarefas;
- associação das tarefas aos respetivos projetos e responsáveis;
- apresentação de estado, prioridade, descrição, tempo registado e datas;
- indicação de tarefas com temporizador ativo;
- estados de loading, erro, retry e lista vazia nas tarefas;
- ligação View task na lista de tarefas e no detalhe do projeto;
- detalhe da tarefa ligado a `GET /api/tasks/{id}`;
- apresentação do projeto, responsável e estado do temporizador;
- ligação do projeto associado ao respetivo detalhe;
- atualização visual do tempo durante uma sessão ativa;
- início, pausa e retoma do temporizador;
- conclusão da tarefa com confirmação;
- mensagens de sucesso e erro nas operações;
- bloqueio do temporizador depois da conclusão;
- tratamento de identificadores inválidos e tarefas inexistentes;
- testes automatizados do armazenamento da autenticação, das rotas protegidas e da configuração de rotas;
- layout responsivo.

O backend aplica autorização de recurso. Uma ação pode ser recusada mesmo quando o controlo ainda está visível no frontend, por exemplo quando o utilizador não é o assignee da tarefa. Uma melhoria futura deve tornar a interface consciente das capabilities devolvidas pela API.

## Estrutura principal

```text
frontend/src/
├── api/
│   ├── apiClient.ts
│   ├── authApi.ts
│   ├── collaboratorsApi.ts
│   ├── dashboardApi.ts
│   ├── projectsApi.ts
│   └── tasksApi.ts
├── auth/
│   ├── AuthContext.ts
│   ├── AuthProvider.tsx
│   ├── authStorage.ts
│   ├── authStorage.test.ts
│   └── useAuth.ts
├── components/
│   └── AppHeader.tsx
├── pages/
│   ├── DashboardPage.tsx
│   ├── LoginPage.tsx
│   ├── NotFoundPage.tsx
│   ├── ProjectDetailPage.tsx
│   ├── ProjectsPage.tsx
│   ├── TaskDetailPage.tsx
│   └── TasksPage.tsx
├── routes/
│   ├── AppRoutes.tsx
│   ├── AppRoutes.test.tsx
│   ├── ProtectedRoute.tsx
│   └── ProtectedRoute.test.tsx
├── test/
│   └── setup.ts
├── types/
│   ├── auth.ts
│   ├── collaborator.ts
│   ├── dashboard.ts
│   ├── project.ts
│   └── task.ts
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
Test Files: 3 passed
Tests: 16 passed
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
