# DevFlow Hub

DevFlow Hub é uma aplicação web académica para gestão de colaboradores, projetos, tarefas, tempo de trabalho e programas internos.

## Estado do repositório

Este repositório representa uma reorganização limpa e estruturada do projeto DevFlow Hub.

O código foi desenvolvido anteriormente num repositório de trabalho. A organização atual tem como objetivos:

- remover ficheiros gerados e configurações locais;
- separar funcionalidades por branches;
- criar commits pequenos e descritivos;
- manter o README e o LOG alinhados com o código;
- preparar uma entrega portátil com JAR, SQL e documentação;
- preservar uma estrutura adequada para avaliação académica.

O histórico reorganizado não pretende alterar as datas reais de desenvolvimento. Os commits deste repositório representam a organização técnica dos principais marcos do projeto.

## Tecnologias utilizadas

### Backend

- Java 21
- Spring Boot
- Spring MVC
- Spring Security
- Spring Data JPA
- Jakarta Validation
- Maven

### Frontend previsto

- Thymeleaf
- React
- TypeScript
- Vite
- Axios

### Base de dados

- PostgreSQL
- H2 para testes de integração

### Testes

- JUnit 5
- Mockito
- MockMvc
- Spring Boot Test
- Smoke tests em PowerShell

## Estratégia de branches

- `main`: versões estáveis e prontas para entrega;
- `develop`: integração das funcionalidades;
- `feature/*`: novas funcionalidades;
- `fix/*`: correções;
- `refactor/*`: melhorias estruturais;
- `test/*`: testes;
- `docs/*`: documentação;
- `chore/*`: configuração, automação e manutenção.

## Estado atual

- Repositório reorganizado com branches e commits descritivos.
- Estrutura PostgreSQL criada e validada numa base de dados limpa.
- Scripts de instalação e migração disponíveis.
- Guia de instalação local da base de dados disponível.
- Backend Spring Boot configurado com Java 21.
- Entidades JPA e repositórios implementados.
- Serviços de colaboradores, projetos, tarefas e programas internos implementados.
- Temporizador das tarefas implementado.
- API REST para colaboradores, projetos, tarefas e programas internos implementada.
- Endpoint de resumo do dashboard implementado.
- Operações REST do temporizador implementadas.
- Tratamento global de erros da API implementado.
- Autenticação JWT implementada.
- Endpoint público de login implementado.
- Endpoints da API protegidos através de Bearer token.
- Expiração e validação de tokens implementadas.
- Colaboradores inativos impedidos de iniciar sessão.
- Alteração de palavra-passe associada ao colaborador autenticado através do JWT.
- JAR executável gerado através do Maven Wrapper.
- Testes unitários do domínio, utilitários e serviços implementados.
- Testes MockMvc do controlador de tarefas implementados.
- Teste de arranque do contexto Spring Boot implementado.
- Perfil de testes configurado com uma base H2 em memória.
- Suite validada com 38 testes, sem falhas, erros ou testes ignorados.
- Backend executado com sucesso ligado a uma base PostgreSQL real.
- Esquema PostgreSQL validado através de `spring.jpa.hibernate.ddl-auto=validate`.
- Endpoints principais da API validados através de smoke tests automatizados.
- CRUD de colaboradores, projetos, tarefas e programas internos validado com PostgreSQL.
- Relações entre colaboradores, projetos, tarefas e gestores validadas.
- Ciclo completo do temporizador das tarefas validado.
- Regras de estado, prioridade, datas e referências inexistentes validadas.
- Respostas HTTP `400`, `401` e `404` validadas.
- Pedidos com JSON malformado devolvem HTTP `400` estruturado, sem stack trace ou detalhes internos.
- Limpeza automática e reposição das contagens iniciais validadas.
- Script PowerShell de smoke tests disponível em `scripts/smoke-test-api.ps1`.

### Trabalho ainda pendente

- Testes de integração executados pelo Maven com uma instância PostgreSQL dedicada.
- Validação de encoding em ambientes adicionais fora do Windows PowerShell 5.1.
- Fluxo seguro de redefinição de palavra-passe para contas sem credenciais conhecidas.
- Interface Thymeleaf.
- Frontend React.
- Validação do JAR final numa instalação independente.

## Autora

Daniela Torres Almeida

## Base de dados

O DevFlow Hub utiliza PostgreSQL para persistir colaboradores, projetos, tarefas e programas internos.

Os ficheiros da base de dados encontram-se em:

```text
database/
├── devflow_hub.sql
├── migrate_existing_database.sql
├── INSTALACAO_BASE_DADOS_LOCAL.txt
└── README.md
```

- `INSTALACAO_BASE_DADOS_LOCAL.txt` explica como preparar a base de dados numa máquina local.
- `devflow_hub.sql` cria uma instalação nova e adiciona dados de demonstração.
- `migrate_existing_database.sql` atualiza bases criadas por versões anteriores.
- `database/README.md` contém as instruções de instalação e migração.

### Validação realizada

O script principal foi executado numa base PostgreSQL vazia chamada `devflow_hub_validation`.

Foram confirmados:

- as tabelas `collaborators`, `projects`, `tasks` e `internal_programs`;
- 6 colaboradores de demonstração;
- 4 projetos;
- 5 tarefas;
- 4 programas internos;
- `internal_programs.manager_id` referencia `collaborators.id`;
- `projects.manager_id` referencia `collaborators.id`;
- `tasks.assignee_id` referencia `collaborators.id`;
- `tasks.project_id` referencia `projects.id`.

### Instalação local

As instruções completas para criar a base de dados numa máquina local estão disponíveis em:

[`database/INSTALACAO_BASE_DADOS_LOCAL.txt`](database/INSTALACAO_BASE_DADOS_LOCAL.txt)

## Backend

O backend do DevFlow Hub utiliza Java 21, Spring Boot, Spring Security, Spring Data JPA e PostgreSQL.

A organização segue uma arquitetura por camadas:

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

- `config`: configurações técnicas partilhadas, incluindo a configuração de segurança;
- `controller`: endpoints REST da aplicação;
- `domain`: estados, prioridades e valores permitidos;
- `dto`: objetos utilizados para transportar pedidos e respostas da API;
- `entity`: entidades JPA associadas às tabelas PostgreSQL;
- `exception`: exceções reutilizáveis e tratamento global de erros;
- `repository`: acesso aos dados com Spring Data JPA;
- `security`: filtros de autenticação JWT e integração com o Spring Security;
- `service`: regras de negócio e consultas agregadas da aplicação;
- `util`: funções comuns de normalização.

### Funcionalidades implementadas

- gestão de colaboradores;
- autenticação de colaboradores através de JWT;
- alteração de palavra-passe mediante autenticação;
- bloqueio de login para colaboradores inativos;
- gestão de projetos e responsáveis;
- gestão de tarefas;
- prioridades `LOW`, `MEDIUM` e `HIGH`;
- estados `PENDING`, `IN_PROGRESS`, `REVIEW` e `COMPLETED`;
- início, pausa, retoma e conclusão do temporizador;
- gestão de programas internos;
- validação das relações entre entidades.

## Configuração e execução

### Variáveis de ambiente

O backend pode ser configurado através das seguintes variáveis:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
SHOW_SQL
JWT_SECRET
JWT_ISSUER
```

Exemplo para PowerShell:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/devflow_hub"
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "<palavra-passe-local-do-postgresql>"
$env:SHOW_SQL = "false"
$env:JWT_SECRET = "<segredo-de-desenvolvimento-com-comprimento-suficiente>"
$env:JWT_ISSUER = "devflow-hub"
```

As credenciais e os segredos não devem ser guardados nem enviados para o repositório Git.

### Compilar o backend

A partir da raiz do repositório:

```powershell
Set-Location ".\backend"
.\mvnw.cmd -DskipTests compile
```

### Iniciar o backend

```powershell
Set-Location ".\backend"
.\mvnw.cmd spring-boot:run
```

O backend deve terminar o arranque com uma mensagem semelhante a:

```text
Started BackendApplication
```

A API fica disponível em:

```text
http://localhost:8080/api
```

## API REST

O DevFlow Hub disponibiliza uma API REST para gerir os principais recursos da aplicação.

### Autenticação

```text
POST /api/auth/login
PUT  /api/auth/change-password
```

O endpoint `POST /api/auth/login` é público e valida as credenciais do colaborador.

Quando as credenciais são válidas, a API devolve um JSON Web Token, JWT, que deve ser utilizado como Bearer token para aceder aos endpoints protegidos.

O endpoint `PUT /api/auth/change-password` requer autenticação. O colaborador é identificado através do campo `sub` do JWT, não sendo aceite um identificador de colaborador fornecido pelo cliente.

Colaboradores inativos não podem iniciar sessão.

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
```

A API também disponibiliza operações específicas para o temporizador:

```text
POST /api/tasks/{id}/start-timer
POST /api/tasks/{id}/pause-timer
POST /api/tasks/{id}/resume-timer
GET  /api/tasks/{id}/total-time
GET  /api/tasks/{id}/timer
POST /api/tasks/{id}/complete
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

O dashboard utiliza DTOs próprios para devolver um resumo dos projetos, tarefas e indicadores da aplicação.

## Autenticação e segurança da API

### Endpoint público

O login é o único endpoint público funcional da API:

```http
POST /api/auth/login
```

Exemplo de pedido:

```json
{
  "email": "ana.silva@devflowhub.pt",
  "password": "DevFlowLocal-2026!"
}
```

Exemplo de resposta:

```json
{
  "accessToken": "<jwt-token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "collaborator": {
    "id": 5,
    "email": "ana.silva@devflowhub.pt",
    "role": "Frontend Developer",
    "active": true
  }
}
```

O token de acesso expira após 900 segundos.

### Endpoints protegidos

Os restantes endpoints em `/api/**` requerem um JWT válido.

O token deve ser enviado no cabeçalho `Authorization`:

```http
Authorization: Bearer <jwt-token>
```

Pedidos sem token, com token inválido ou com token expirado são rejeitados com HTTP `401`.

Exemplo em PowerShell:

```powershell
$loginBody = @{
    email = "ana.silva@devflowhub.pt"
    password = "DevFlowLocal-2026!"
} | ConvertTo-Json

$loginResponse = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/auth/login" `
    -ContentType "application/json" `
    -Body $loginBody

$token = $loginResponse.accessToken

Invoke-RestMethod `
    -Method Get `
    -Uri "http://localhost:8080/api/tasks" `
    -Headers @{
        Authorization = "Bearer $token"
    }
```

### Alteração da palavra-passe

O colaborador autenticado é identificado através da claim `sub` do JWT.

O endpoint de alteração da palavra-passe não aceita um identificador de colaborador fornecido pelo cliente. Esta regra impede que um colaborador tente alterar a palavra-passe de outra conta através da modificação do pedido.

Depois da alteração, o colaborador pode iniciar sessão com a nova palavra-passe.

### Colaboradores inativos

Colaboradores marcados como inativos não podem iniciar sessão, mesmo quando o email e a palavra-passe enviados estão corretos.

## Tratamento de erros

A API possui tratamento global de exceções para:

- recursos não encontrados;
- operações inválidas;
- credenciais inválidas;
- pedidos não autenticados;
- erros de validação;
- JSON malformado;
- erros inesperados da aplicação.

As respostas de erro não expõem stack traces, hashes de palavras-passe ou detalhes internos da aplicação.

## Empacotamento

O backend pode ser compilado e empacotado através do Maven Wrapper:

```powershell
Set-Location ".\backend"
.\mvnw.cmd clean package -DskipTests
```

O JAR executável é gerado em:

```text
backend/target/devflow-hub.jar
```

## Testes automatizados

O backend utiliza JUnit 5, Mockito, MockMvc, Spring Boot Test e H2.

Os testes atuais abrangem:

- valores e regras da camada de domínio;
- normalização de texto;
- regras dos colaboradores;
- regras dos projetos;
- regras dos programas internos;
- agregação de dados do dashboard;
- estados e temporizador das tarefas;
- endpoints do controller de tarefas;
- arranque completo do contexto Spring Boot;
- configuração JPA com uma base H2 em memória.

A suite completa pode ser executada com:

```powershell
Set-Location ".\backend"
.\mvnw.cmd clean test
```

Na validação realizada em 23/07/2026, foram executados:

```text
Tests run: 38
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O perfil de testes utiliza:

```text
backend/src/test/resources/application-test.properties
```

A base H2 é criada apenas em memória durante os testes e não substitui a validação final com PostgreSQL.

## Smoke tests da API com PostgreSQL

O repositório inclui um script PowerShell para validar a API REST com uma base PostgreSQL local.

O script encontra-se em:

```text
scripts/smoke-test-api.ps1
```

### Pré-requisitos

- Java 21;
- PostgreSQL em execução localmente;
- base de dados `devflow_hub` criada;
- esquema da base de dados instalado;
- porta `8080` disponível;
- variáveis de ambiente da base de dados e do JWT configuradas.

### Executar os smoke tests

Com o backend em execução, abre um segundo terminal PowerShell na raiz do repositório e executa:

```powershell
powershell.exe `
    -NoProfile `
    -ExecutionPolicy Bypass `
    -File ".\scripts\smoke-test-api.ps1"
```

O script valida:

- geração de JWT após um login válido;
- rejeição de pedidos sem token;
- rejeição de tokens inválidos;
- acesso aos endpoints protegidos com um Bearer token válido;
- rejeição de colaboradores inativos com HTTP `401`;
- alteração de palavra-passe através do colaborador identificado no claim `sub`;
- login com a nova palavra-passe;
- rejeição da palavra-passe anterior;
- ocultação da palavra-passe e do respetivo hash nas respostas;
- endpoints principais de colaboradores, projetos, tarefas, programas internos e dashboard;
- presença dos principais campos do dashboard;
- CRUD completo de colaboradores, projetos, tarefas e programas internos;
- relações de gestor, projeto e responsável;
- estados e prioridades permitidos;
- datas iniciais e finais;
- campos obrigatórios e referências inexistentes;
- proteção dos campos internos do temporizador durante a criação;
- início, pausa, retoma e conclusão do temporizador;
- acumulação do tempo de diferentes sessões;
- proteção do estado enquanto o temporizador está ativo;
- rejeição da pausa de um temporizador inativo;
- rejeição do reinício do temporizador de uma tarefa concluída;
- rejeição de JSON malformado com resposta estruturada;
- ausência de `trace`, `error`, `exception` e `path` nas respostas de erro;
- respostas estruturadas HTTP `400`, `401` e `404`;
- eliminação dos recursos temporários por ordem de dependência;
- reposição das contagens iniciais após a execução;
- limpeza automática dos dados temporários em caso de falha.

Uma execução bem-sucedida termina com:

```text
All PostgreSQL API smoke tests passed.
```

### Utilizar outro endereço da API

O endereço da API pode ser alterado através do parâmetro `BaseUrl`:

```powershell
powershell.exe `
    -NoProfile `
    -ExecutionPolicy Bypass `
    -File ".\scripts\smoke-test-api.ps1" `
    -BaseUrl "http://localhost:8080"
```

O script devolve um código de saída diferente de zero quando algum teste falha.
