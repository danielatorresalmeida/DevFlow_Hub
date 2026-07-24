# DevFlow Hub Frontend

Frontend React do DevFlow Hub, responsável pela autenticação do utilizador, navegação protegida e apresentação dos dados fornecidos pela API Spring Boot.

## Tecnologias

- React 19
- TypeScript 6
- Vite 8
- React Router 7
- Fetch API nativa
- ESLint

## Funcionalidades atuais

- página de login ligada a `POST /api/auth/login`;
- sessão JWT guardada em `sessionStorage`;
- rotas `/dashboard` e `/projects` protegidas;
- persistência da sessão após atualização da página;
- logout manual e expiração automática;
- inclusão automática do Bearer token nos pedidos autenticados;
- tratamento global de HTTP `401`;
- dashboard ligado a `GET /api/dashboard`;
- indicadores, tarefas recentes e projetos próximos;
- estados de loading, erro, retry e ausência de dados;
- cabeçalho autenticado reutilizável;
- navegação entre dashboard e projetos;
- lista autenticada de projetos;
- associação dos projetos aos respetivos gestores;
- apresentação de estado, descrição, gestor e datas;
- estados de loading, erro, retry e lista vazia nos projetos;
- layout responsivo.

## Estrutura principal

```text
frontend/src/
├── api/
│   ├── apiClient.ts
│   ├── authApi.ts
│   ├── collaboratorsApi.ts
│   ├── dashboardApi.ts
│   └── projectsApi.ts
├── auth/
│   ├── AuthContext.ts
│   ├── AuthProvider.tsx
│   ├── authStorage.ts
│   └── useAuth.ts
├── components/
│   └── AppHeader.tsx
├── pages/
│   ├── DashboardPage.tsx
│   ├── LoginPage.tsx
│   ├── NotFoundPage.tsx
│   └── ProjectsPage.tsx
├── routes/
│   ├── AppRoutes.tsx
│   └── ProtectedRoute.tsx
├── types/
│   ├── auth.ts
│   ├── collaborator.ts
│   ├── dashboard.ts
│   └── project.ts
├── index.css
└── main.tsx
```

## Pré-requisitos

- Node.js compatível com Vite 8;
- npm;
- backend DevFlow Hub em execução para testar login e dashboard.

## Instalação

```powershell
Set-Location ".\frontend"
npm install
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

O ficheiro `.env.local` não deve ser enviado para o Git.

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
npm run build
```

O build de produção é criado em `dist/`.

## Rotas atuais

```text
/login       pública
/dashboard   protegida
/projects    protegida
*            página não encontrada
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

## Scripts npm

```text
npm run dev       inicia o servidor Vite
npm run lint      executa o ESLint
npm run build     valida TypeScript e gera o build
npm run preview   pré-visualiza o build localmente
```
