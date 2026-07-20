# DevFlow Hub

DevFlow Hub é uma aplicação web académica para gestão de colaboradores, projetos, tarefas, tempo de trabalho e programas internos.

## Estado do repositório

Este repositório representa uma reorganização limpa e estruturada do projeto DevFlow Hub.

O código foi desenvolvido anteriormente num repositório de trabalho. A nova organização tem como objetivos:

- remover ficheiros gerados e configurações locais;
- separar as funcionalidades por branches;
- criar commits pequenos e descritivos;
- manter o README e o LOG alinhados com o código;
- preparar uma entrega portátil com JAR, SQL e documentação;
- preservar uma estrutura adequada para avaliação académica.

O histórico reorganizado não pretende alterar as datas reais de desenvolvimento. Os commits deste repositório representam a organização técnica dos principais marcos do projeto.

## Tecnologias do projeto

### Backend

- Java 21
- Spring Boot
- Spring MVC
- Thymeleaf
- Spring Data JPA
- Jakarta Validation
- Maven

### Frontend complementar

- React
- TypeScript
- Vite
- Axios

### Base de dados

- PostgreSQL

### Testes

- JUnit 5
- Mockito
- MockMvc
- Spring Boot Test
- H2 para testes de integração

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
- Entidades JPA e repositories implementados.
- Services de colaboradores, projetos, tarefas e programas internos implementados.
- Temporizador das tarefas implementado.
- Backend compilado com sucesso através do Maven Wrapper.
- API REST para colaboradores, projetos, tarefas e programas internos implementada.
- Endpoint de resumo do dashboard implementado.
- Operações REST do temporizador implementadas.
- Tratamento global dos erros da API implementado.
- JAR executável gerado através do Maven Wrapper.
- Testes unitários do domínio, utilitários e services implementados.
- Testes MockMvc do controller de tarefas implementados.
- Teste de arranque do contexto Spring Boot implementado.
- Perfil de testes configurado com uma base H2 em memória.
- Suite validada com 18 testes, sem falhas ou erros.

Ainda estão pendentes:

- execução do backend ligado ao PostgreSQL;
- validação dos endpoints REST com Postman ou ferramenta equivalente;
- expansão da cobertura dos restantes controllers REST;
- testes de integração dos endpoints com persistência;
- validação dos pedidos com Jakarta Validation;
- interface Thymeleaf;
- frontend React;
- validação do JAR final numa instalação independente.

## Autora

Daniela Torres Almeida

## Base de dados

O DevFlow Hub utiliza PostgreSQL para persistir colaboradores, projetos,
tarefas e programas internos.

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

O script principal foi executado numa base PostgreSQL vazia chamada
`devflow_hub_validation`.

Foram confirmadas:

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

As instruções completas para criar a base de dados numa máquina local
estão disponíveis em:

[`database/INSTALACAO_BASE_DADOS_LOCAL.txt`](database/INSTALACAO_BASE_DADOS_LOCAL.txt)

## Backend

O backend do DevFlow Hub utiliza Java 21, Spring Boot, Spring Data JPA e
PostgreSQL.

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
├── service/
└── util/
```

### Responsabilidades

- `config`: configurações técnicas partilhadas;
- `controller`: endpoints REST da aplicação;
- `domain`: estados, prioridades e valores permitidos;
- `dto`: objetos utilizados para transportar respostas específicas da API;
- `entity`: entidades JPA associadas às tabelas PostgreSQL;
- `exception`: exceções reutilizáveis e tratamento global de erros;
- `repository`: acesso aos dados com Spring Data JPA;
- `service`: regras de negócio e consultas agregadas da aplicação;
- `util`: funções comuns de normalização.

### Funcionalidades implementadas

- gestão de colaboradores;
- lógica de autenticação simples de colaboradores;
- gestão de projetos e responsáveis;
- gestão de tarefas;
- prioridades `LOW`, `MEDIUM` e `HIGH`;
- estados `PENDING`, `IN_PROGRESS`, `REVIEW` e `COMPLETED`;
- início, pausa, retoma e conclusão do temporizador;
- gestão de programas internos;
- validação das relações entre entidades.

### Compilação

A compilação pode ser validada através do Maven Wrapper:

```cmd
cd backend
.\mvnw.cmd -DskipTests compile
```

O backend foi compilado com sucesso utilizando Java 21.

## API REST

O DevFlow Hub disponibiliza uma API REST para gerir os principais recursos da
aplicação.

### Endpoints principais

#### Colaboradores

```text
GET    /api/collaborators
GET    /api/collaborators/{id}
POST   /api/collaborators
PUT    /api/collaborators/{id}
DELETE /api/collaborators/{id}
```

#### Projetos

```text
GET    /api/projects
GET    /api/projects/{id}
POST   /api/projects
PUT    /api/projects/{id}
DELETE /api/projects/{id}
```

#### Tarefas

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

#### Programas internos

```text
GET    /api/internal-programs
GET    /api/internal-programs/{id}
POST   /api/internal-programs
PUT    /api/internal-programs/{id}
DELETE /api/internal-programs/{id}
```

#### Dashboard

```text
GET /api/dashboard
```

O dashboard utiliza DTOs próprios para devolver um resumo dos projetos,
tarefas e indicadores da aplicação.

### Tratamento de erros

A API possui tratamento global de exceções para:

- recursos não encontrados;
- operações inválidas;
- erros de validação;
- erros inesperados da aplicação.

### Empacotamento

O backend pode ser compilado e empacotado através do Maven Wrapper:

```powershell
cd backend
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
cd backend
.\mvnw.cmd clean test
```

Na validação realizada em 20/07/2026, foram executados:

```text
Tests run: 18
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O perfil de testes utiliza:

```text
backend/src/test/resources/application-test.properties
```

A base H2 é criada apenas em memória durante os testes e não substitui a
validação final com PostgreSQL.