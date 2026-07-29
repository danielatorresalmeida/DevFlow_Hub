# DevFlow Hub - Registo de Desenvolvimento

## Nota sobre a reorganização

Este LOG foi reconstruído de forma organizada com base no código-fonte existente, nos documentos das sprints, nos registos de desenvolvimento anteriores e nas validações realizadas.

O repositório anterior permanece como histórico do trabalho original. Este novo repositório organiza os principais marcos técnicos através de branches e commits coerentes.

As datas dos novos commits representam a data da reorganização no Git e não substituem as datas reais registadas nas sessões originais.

---

## Marco 0 - 17/07/2026 - Criação do novo repositório organizado

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

### Estatísticas do Git

Segundo as estatísticas do Git, este marco adicionou **5 ficheiros** e
**259 linhas**.

A contagem inclui os ficheiros de configuração e documentação utilizados para
criar a fundação do novo repositório.

---

## Marco 1 - 17/07/2026 - Estrutura PostgreSQL e migração

### Objetivo da sessão

Adicionar ao novo repositório a estrutura persistente do DevFlow Hub e
documentar a criação e atualização da base de dados PostgreSQL.

### Funcionalidades implementadas

- Adição do script principal `devflow_hub.sql`.
- Criação das tabelas `collaborators`, `projects`, `tasks` e
  `internal_programs`.
- Inclusão das relações entre colaboradores, projetos, tarefas e programas.
- Utilização de identificadores `BIGINT`, compatíveis com `Long` no backend.
- Inclusão de campos de auditoria.
- Inclusão dos campos utilizados pelo temporizador das tarefas.
- Inclusão de constraints, índices e dados de demonstração.
- Adição do script `migrate_existing_database.sql`.
- Criação de um guia específico para preparar e atualizar o PostgreSQL.

### Problemas encontrados

- A base de dados de trabalho já continha tabelas criadas por versões
  anteriores.
- `CREATE TABLE IF NOT EXISTS` não recria tabelas existentes nem adiciona
  constraints que estejam em falta.
- A primeira consulta às foreign keys não apresentou resultados na base
  antiga.

### Como resolvi

- Mantive a base de trabalho sem eliminar os dados existentes.
- Criei uma base vazia chamada `devflow_hub_validation`.
- Executei o script principal numa instalação limpa.
- Validei separadamente as tabelas, os dados de demonstração e as foreign
  keys.
- Mantive um script específico para a migração de bases anteriores.

### Ficheiros atualizados

- `database/devflow_hub.sql`
- `database/migrate_existing_database.sql`
- `database/README.md`
- `README.md`
- `LOG.md`

### Estado atual

- As quatro tabelas principais foram criadas corretamente.
- Foram carregados 6 colaboradores, 4 projetos, 5 tarefas e 4 programas
  internos.
- Foram validadas quatro foreign keys:
  - `internal_programs.manager_id` referencia `collaborators.id`;
  - `projects.manager_id` referencia `collaborators.id`;
  - `tasks.assignee_id` referencia `collaborators.id`;
  - `tasks.project_id` referencia `projects.id`.
- O script principal está preparado para instalações novas.
- O script de migração está disponível para bases antigas.
- Nenhuma password administrativa real foi adicionada ao repositório.

### Validação ainda pendente

- Executar `migrate_existing_database.sql` numa cópia de uma base antiga.
- Confirmar o esquema através do backend com `ddl-auto=validate`.

### Próxima etapa

Adicionar a fundação do backend Spring Boot, incluindo Maven, configuração
segura, entidades e repositories.

### Estatísticas do Git

Segundo as estatísticas do Git, este marco adicionou **5 ficheiros** e
**465 linhas**.

A contagem inclui os scripts SQL, a documentação da base de dados e as
atualizações associadas no README e no LOG.

---

## Atualização - Guia de instalação local da base de dados

### Objetivo da sessão

Criar um documento simples com as instruções necessárias para preparar a base
de dados PostgreSQL numa máquina local.

### Funcionalidades implementadas

- Criação de um guia TXT para instalação local do PostgreSQL.
- Documentação da criação da base `devflow_hub`.
- Instruções para executar o script `devflow_hub.sql`.
- Instruções para configurar `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.
- Inclusão de consultas para validar tabelas, dados e chaves estrangeiras.
- Documentação de erros frequentes e respetivas soluções.

### Problemas encontrados

- Era necessário fornecer instruções independentes do README principal.
- As credenciais do PostgreSQL variam entre computadores.

### Como resolvi

- Criei um ficheiro TXT dedicado à instalação local.
- Mantive as passwords reais fora do repositório.
- Adicionei uma referência ao guia no README principal.

### Ficheiros atualizados

- `database/INSTALACAO_BASE_DADOS_LOCAL.txt`
- `README.md`
- `LOG.md`

### Estado atual

- O projeto possui instruções para criar a base de dados localmente.
- O utilizador pode configurar as suas próprias credenciais.
- O guia inclui validação das quatro tabelas e das foreign keys.

### Validação ainda pendente

- Testar o guia completo numa segunda máquina ou instalação PostgreSQL limpa.

### Próxima etapa

Continuar a implementação da branch `feature/backend-core`.

### Estatísticas do Git

Segundo as estatísticas do Git, esta atualização alterou **3 ficheiros**, com
**371 linhas adicionadas** e **1 linha removida**.

A contagem inclui o guia de instalação local e as atualizações correspondentes
no README e no LOG.

---

## Marco 2 - 20/07/2026 - Fundação e serviços do backend

### Objetivo da sessão

Adicionar ao novo repositório a fundação Spring Boot e organizar as principais
regras de negócio do DevFlow Hub através de entidades, repositories e services.

### Funcionalidades implementadas

- Configuração do backend com Java 21 e Spring Boot.
- Adição do Maven Wrapper.
- Configuração segura da ligação PostgreSQL através de variáveis de ambiente.
- Criação das entidades `Collaborator`, `Project`, `Task` e
  `InternalProgram`.
- Criação dos repositories Spring Data JPA.
- Centralização dos estados e prioridades permitidos.
- Criação de uma função comum para normalização de texto.
- Adição de exceções reutilizáveis.
- Implementação das regras de negócio dos colaboradores.
- Implementação da lógica de autenticação simples de colaboradores.
- Implementação das regras de negócio dos projetos.
- Implementação das regras de negócio das tarefas.
- Implementação das ações de iniciar, pausar, retomar e concluir o timer.
- Implementação das regras de negócio dos programas internos.
- Injeção de `Clock` para tornar a lógica temporal testável.

### Problemas encontrados

- O Java configurado anteriormente dependia de uma pasta interna de uma
  extensão do VS Code.
- A pasta utilizada pela extensão deixou de existir.
- O Maven Wrapper não conseguia encontrar uma instalação válida através de
  `JAVA_HOME`.
- Os valores de estados e prioridades estavam distribuídos por diferentes
  partes da aplicação.
- A lógica temporal precisava de ser testável sem depender diretamente da hora
  real do computador.

### Como resolvi

- Instalei o Eclipse Temurin JDK 21 através do WinGet.
- Configurei permanentemente `JAVA_HOME`.
- Atualizei o `PATH` e as configurações Java do VS Code.
- Removi dependências de caminhos temporários da extensão Pleiades.
- Centralizei estados e prioridades na camada de domínio.
- Mantive os repositories responsáveis apenas pelo acesso aos dados.
- Mantive as regras da aplicação dentro dos services.
- Injetei um `Clock` configurável no service de tarefas.
- Validei a compilação através do Maven Wrapper.

### Ficheiros atualizados

- `backend/.mvn/`
- `backend/mvnw`
- `backend/mvnw.cmd`
- `backend/pom.xml`
- `backend/src/main/java/com/devflowhub/backend/BackendApplication.java`
- `backend/src/main/java/com/devflowhub/backend/config/`
- `backend/src/main/java/com/devflowhub/backend/domain/`
- `backend/src/main/java/com/devflowhub/backend/entity/`
- `backend/src/main/java/com/devflowhub/backend/exception/`
- `backend/src/main/java/com/devflowhub/backend/repository/`
- `backend/src/main/java/com/devflowhub/backend/service/`
- `backend/src/main/java/com/devflowhub/backend/util/`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-local.example.properties`
- `README.md`
- `LOG.md`

### Estado atual

- A fundação Spring Boot está presente.
- Java 21 está instalado e configurado.
- As entidades estão alinhadas com o esquema PostgreSQL.
- Os repositories estão disponíveis.
- As principais regras de negócio estão implementadas.
- O temporizador das tarefas está implementado.
- O backend compila com sucesso através do Maven Wrapper.
- Nenhuma password administrativa foi adicionada ao repositório.

### Validação ainda pendente

- Adicionar testes unitários dos services.
- Executar o backend ligado à base PostgreSQL.
- Validar o esquema com `ddl-auto=validate`.
- Adicionar os controllers REST.
- Adicionar o tratamento global dos erros da API.
- Adicionar os controllers MVC e a interface Thymeleaf.

### Próxima etapa

Criar a branch `feature/rest-api` e implementar os endpoints REST da aplicação.

### Estatísticas do Git

Segundo as estatísticas do Git, a pasta `backend` adicionou **24 ficheiros** e
**1 855 linhas**.

A contagem inclui código Java, Maven Wrapper, configuração Spring Boot e
ficheiros de propriedades.

---

## Marco 3 - 20/07/2026 - Implementação da API REST

### Objetivo da sessão

Adicionar uma API REST ao DevFlow Hub para expor as funcionalidades já
implementadas na camada de serviços.

### Funcionalidades implementadas

- Criação dos DTOs utilizados pelo dashboard.
- Criação do serviço de consulta e agregação do dashboard.
- Criação do controller REST dos colaboradores.
- Criação do controller REST dos projetos.
- Criação do controller REST das tarefas.
- Criação do controller REST dos programas internos.
- Criação do controller REST do dashboard.
- Implementação de endpoints CRUD.
- Implementação dos endpoints do temporizador das tarefas.
- Implementação do endpoint para concluir uma tarefa.
- Criação de uma estrutura comum para erros da API.
- Implementação do tratamento global das exceções.

### Endpoints adicionados

- `/api/collaborators`
- `/api/projects`
- `/api/tasks`
- `/api/internal-programs`
- `/api/dashboard`
- `/api/tasks/{id}/start-timer`
- `/api/tasks/{id}/pause-timer`
- `/api/tasks/{id}/resume-timer`
- `/api/tasks/{id}/total-time`
- `/api/tasks/{id}/timer`
- `/api/tasks/{id}/complete`

### Problemas encontrados

- O `DashboardController` dependia de um `DashboardService` que ainda não
  estava presente no novo repositório.
- A primeira compilação dos controllers terminou com erro porque essa classe
  não podia ser encontrada.
- Era necessário manter os controllers REST separados dos controllers MVC da
  interface Thymeleaf.

### Como resolvi

- Localizei o `DashboardService` no código-fonte original.
- Adicionei o serviço num commit separado.
- Validei que o serviço utiliza os repositories e DTOs já presentes.
- Mantive os controllers REST na pasta `controller`.
- Não adicionei ainda os controllers MVC existentes em `web/controller`.
- Compilei novamente o backend depois de adicionar a dependência em falta.
- Mantive o tratamento global de erros num commit independente.

### Ficheiros atualizados

- `backend/src/main/java/com/devflowhub/backend/controller/CollaboratorController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/DashboardController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/InternalProgramController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/ProjectController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/TaskController.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardProjectItem.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardSummary.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardTaskItem.java`
- `backend/src/main/java/com/devflowhub/backend/exception/ApiError.java`
- `backend/src/main/java/com/devflowhub/backend/exception/ApiExceptionHandler.java`
- `backend/src/main/java/com/devflowhub/backend/service/DashboardService.java`
- `README.md`
- `LOG.md`

### Estado atual

- Os principais recursos possuem controllers REST.
- As operações CRUD estão expostas através da API.
- As operações do temporizador estão disponíveis através de endpoints
  específicos.
- O dashboard possui DTOs e serviço próprios.
- As exceções da aplicação são tratadas globalmente.
- O backend compila e pode ser empacotado através do Maven Wrapper.
- O ficheiro `backend/target/devflow-hub.jar` foi gerado.
- Na validação realizada em 20/07/2026, o JAR gerado possuía
  `57 700 076` bytes.
- A pasta `target` permaneceu fora do controlo de versões.

### Validação ainda pendente

- Executar a aplicação ligada ao PostgreSQL.
- Testar os endpoints com Postman ou ferramenta equivalente.
- Adicionar testes unitários dos services.
- Adicionar testes dos controllers com MockMvc.
- Validar os pedidos com Jakarta Validation.
- Adicionar os controllers MVC e a interface Thymeleaf.
- Validar o JAR numa máquina diferente.

### Próxima etapa

Criar a branch `test/backend-api` para adicionar testes unitários dos services
e testes dos controllers REST com MockMvc. Depois, executar a aplicação ligada
ao PostgreSQL e validar os endpoints através do Postman.

### Estatísticas do Git

Segundo as estatísticas do Git, a implementação da API REST adicionou
**11 ficheiros** e **528 linhas**.

A contagem inclui controllers REST, DTOs, tratamento de exceções e o serviço do
dashboard. As alterações no README e no LOG são contabilizadas separadamente
no commit documental.

---

## Marco 4 - 20/07/2026 - Testes automatizados do backend

### Objetivo da sessão

Adicionar testes automatizados para validar as principais regras de negócio,
o controller REST de tarefas e o arranque do contexto Spring Boot.

### Testes implementados

- Testes dos valores permitidos da camada de domínio.
- Teste da normalização comum de texto.
- Testes do service de colaboradores.
- Testes do service de projetos.
- Testes do service de programas internos.
- Teste do service do dashboard.
- Testes do service de tarefas.
- Testes das operações temporais das tarefas.
- Testes MockMvc do controller de tarefas.
- Teste de arranque do contexto Spring Boot.
- Configuração de uma base H2 em memória para o perfil `test`.

### Componentes validados

- `DomainValues`
- `TextNormalizer`
- `CollaboratorService`
- `ProjectService`
- `InternalProgramService`
- `DashboardService`
- `TaskService`
- `TaskController`
- configuração Spring Boot;
- repositories Spring Data JPA;
- ligação JPA à base H2 em memória.

### Problemas encontrados

- Inicialmente, a branch não continha ficheiros de teste.
- O Maven indicava que não existiam testes para executar.
- Os testes do projeto original estavam misturados entre API REST, services,
  interface MVC e segurança.
- Os testes MVC e de segurança dependiam de componentes que ainda não tinham
  sido adicionados ao novo repositório.
- O contexto Spring apresentou um aviso porque a pasta de templates Thymeleaf
  ainda não existe.
- O Mockito apresentou avisos sobre o carregamento dinâmico do Java Agent em
  versões futuras do Java.

### Como resolvi

- Adicionei os testes progressivamente e em commits separados.
- Mantive fora desta branch os testes da interface MVC e do interceptor de
  autenticação.
- Executei cada grupo de testes antes do respetivo commit.
- Configurei o perfil `test` com uma base H2 em memória.
- Adicionei um teste de contexto com `@SpringBootTest`.
- Executei a suite completa através do Maven Wrapper.
- Confirmei que os avisos do Thymeleaf e do Mockito não representam falhas
  nesta etapa.

### Ficheiros adicionados e atualizados

- `backend/src/test/java/com/devflowhub/backend/BackendApplicationTests.java`
- `backend/src/test/java/com/devflowhub/backend/controller/TaskControllerTest.java`
- `backend/src/test/java/com/devflowhub/backend/domain/DomainValuesTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/CollaboratorServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/DashboardServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/InternalProgramServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/ProjectServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/TaskServiceTest.java`
- `backend/src/test/java/com/devflowhub/backend/util/TextNormalizerTest.java`
- `backend/src/test/resources/application-test.properties`
- `README.md`
- `LOG.md`

### Resultado da validação

A suite completa foi executada com:

```powershell
.\mvnw.cmd clean test
```

Resultado:

```text
Tests run: 18
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O teste de contexto confirmou:

- perfil `test` ativo;
- ligação à base `jdbc:h2:mem:devflow_hub_test`;
- quatro repositories JPA encontrados;
- inicialização do `EntityManagerFactory`;
- arranque completo do contexto Spring Boot.

### Estado atual

- As principais regras de negócio possuem testes unitários.
- A lógica temporal das tarefas possui testes automatizados.
- O controller de tarefas possui testes MockMvc.
- O contexto Spring Boot inicia corretamente com H2.
- A suite completa possui 18 testes.
- Todos os testes passaram sem falhas ou erros.

### Validação ainda pendente

- Adicionar testes dos restantes controllers REST.
- Adicionar testes de persistência dos repositories.
- Executar testes de integração com PostgreSQL.
- Validar os endpoints através do Postman.
- Adicionar os testes MVC quando a interface Thymeleaf for integrada.
- Resolver preventivamente o aviso futuro do Java Agent do Mockito.

### Próxima etapa

Executar o backend ligado ao PostgreSQL, validar o esquema com
`ddl-auto=validate` e testar os endpoints REST através do Postman.

### Estatísticas do Git

Segundo as estatísticas do Git, a implementação dos testes adicionou
**10 ficheiros** e **579 linhas**.

A contagem inclui testes unitários, testes MockMvc, o teste de contexto Spring
e a configuração H2. As alterações no README e no LOG são contabilizadas
separadamente no commit documental.

---

## Marco 5 - 20/07/2026 - Validação da API com PostgreSQL

### Objetivo da sessão

Validar o backend DevFlow Hub ligado a uma base de dados PostgreSQL local,
confirmar a compatibilidade entre as entidades JPA e o esquema existente e
criar um smoke test reproduzível para os principais endpoints REST.

### Validações realizadas

- Ligação ao PostgreSQL através do JDBC.
- Inicialização do pool de ligações HikariCP.
- Deteção do PostgreSQL 18.3 e do dialecto `PostgreSQLDialect`.
- Validação do esquema através de
  `spring.jpa.hibernate.ddl-auto=validate`.
- Inicialização do `EntityManagerFactory`.
- Arranque do Tomcat na porta `8080`.
- Validação dos endpoints:
  - `GET /api/collaborators`;
  - `GET /api/projects`;
  - `GET /api/tasks`;
  - `GET /api/internal-programs`;
  - `GET /api/dashboard`.
- Validação da presença dos principais campos do dashboard.
- Validação de um recurso inexistente com resposta HTTP `404`.
- Validação de um pedido inválido com resposta HTTP `400`.
- Validação do CRUD de colaboradores:
  - criação;
  - consulta por ID;
  - atualização;
  - eliminação.
- Confirmação de que a password é aceite nos pedidos de criação, mas não é
  devolvida nas respostas da API.
- Confirmação de que uma atualização sem password preserva a password
  existente.
- Confirmação da remoção do colaborador temporário após o teste.
- Criação de um script PowerShell para repetir automaticamente as validações.

### Problemas encontrados

- As variáveis de ambiente da base de dados tinham de ser definidas na mesma
  sessão PowerShell utilizada para iniciar o backend.
- Um pedido de criação de colaborador sem password devolveu corretamente
  HTTP `400`.
- O `Invoke-WebRequest` apresentou um aviso de segurança ao processar a
  resposta do pedido `DELETE`.
- Alguns fragmentos de comandos colados no terminal criaram ficheiros vazios
  e não rastreados na raiz do repositório.
- A primeira versão do script de smoke tests continha erros de sintaxe
  causados pela formatação do conteúdo copiado.
- Foram identificados registos antigos com caracteres portugueses corrompidos,
  como `MÃ³dulo`, `GestÃ£o` e `FormaÃ§Ã£o`.

### Como resolvi

- Defini `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` antes de executar o Maven
  Wrapper na mesma sessão PowerShell.
- Consultei a resposta JSON do erro para confirmar a regra obrigatória da
  password.
- Confirmei a eliminação do colaborador através de uma consulta posterior que
  devolveu HTTP `404`.
- Removi apenas os ficheiros vazios criados acidentalmente e confirmei que o
  Maven Wrapper verdadeiro permaneceu em `backend/mvnw.cmd`.
- Corrigi e validei a sintaxe do script PowerShell.
- Adicionei limpeza automática do colaborador temporário através de um bloco
  `finally`.
- Mantive a correção dos dados antigos com problemas de codificação como uma
  tarefa separada, para não misturar alterações de dados com a validação da
  API.

### Ficheiros adicionados e atualizados

- `scripts/smoke-test-api.ps1`
- `README.md`
- `LOG.md`

### Resultado da validação

O backend foi executado com sucesso utilizando PostgreSQL real.

```text
Database: PostgreSQL 18.3
API port: 8080
Schema validation: successful
Main GET endpoints: successful
Dashboard validation: successful
Collaborator CRUD: successful
Error handling 400/404: successful
Temporary data cleanup: successful
```

O smoke test foi executado com:

```powershell
powershell.exe `
    -NoProfile `
    -ExecutionPolicy Bypass `
    -File ".\scripts\smoke-test-api.ps1"
```

Resultado final:

```text
All API smoke tests passed.
```

### Estado atual

- O backend inicia corretamente ligado ao PostgreSQL.
- As entidades JPA são compatíveis com o esquema existente.
- Os cinco endpoints principais respondem corretamente.
- O dashboard devolve os indicadores esperados.
- O tratamento global de erros devolve respostas estruturadas para HTTP
  `400` e `404`.
- O CRUD de colaboradores foi validado com persistência real.
- A password não é exposta nas respostas JSON.
- O script de smoke tests remove os dados temporários criados durante a
  execução.
- Nenhuma credencial real foi adicionada ao repositório.

### Validação ainda pendente

- Expandir os smoke tests para projetos, tarefas e programas internos.
- Validar os endpoints do temporizador com PostgreSQL.
- Adicionar testes automatizados de integração com persistência PostgreSQL.
- Expandir os testes dos restantes controllers REST.
- Corrigir os registos antigos com caracteres corrompidos.
- Avaliar o armazenamento seguro das passwords antes de implementar
  autenticação para utilização real.
- Validar o JAR final numa instalação independente.

### Próxima etapa

Expandir a validação da API para o CRUD de projetos, tarefas e programas
internos, incluindo as operações do temporizador. Depois, tratar separadamente
os dados antigos com problemas de codificação.

### Estatísticas do Git

As estatísticas deste marco devem ser registadas depois da criação do commit,
para refletirem os valores reais apresentados pelo Git.

---

## Marco 6 - 21/07/2026 - Validação CRUD completa com PostgreSQL

### Objetivo

Expandir o smoke test da API para validar os principais recursos com
persistência PostgreSQL real, incluindo relações, regras de negócio e o ciclo
completo do temporizador das tarefas.

### Validação realizada

Foram validados:

- CRUD de colaboradores, projetos, tarefas e programas internos;
- relações entre colaboradores, gestores, projetos e responsáveis;
- estados e prioridades permitidos;
- validação das datas iniciais e finais;
- campos obrigatórios e referências inexistentes;
- proteção dos campos internos do temporizador durante a criação;
- início, pausa, retoma e conclusão do temporizador;
- acumulação do tempo de várias sessões;
- proteção do estado durante um temporizador ativo;
- rejeição da pausa de um temporizador inativo;
- rejeição do reinício de uma tarefa concluída;
- respostas HTTP `400` e `404`;
- eliminação e limpeza dos recursos temporários.

### Smoke test automatizado

O ficheiro `scripts/smoke-test-api.ps1` foi expandido para criar e validar
temporariamente:

- um colaborador;
- um projeto;
- um programa interno;
- uma tarefa.

Os recursos são eliminados por ordem de dependência:

1. tarefa;
2. programa interno;
3. projeto;
4. colaborador.

O bloco `finally` também remove os recursos temporários caso ocorra uma falha
durante a execução.

### Correção no PowerShell

A conversão dos payloads JSON para UTF-8 devolvia o array de bytes elemento por
elemento através do pipeline.

A função foi corrigida com `Write-Output -NoEnumerate`, garantindo que o
`Invoke-RestMethod` recebe o array completo.

### Resultado

- todos os testes PostgreSQL da API passaram;
- código de saída: `0`;
- as contagens regressaram aos valores iniciais;
- nenhum dado temporário permaneceu na base de dados;
- nenhuma credencial real foi adicionada ao repositório.

Mensagem final:

```text
All PostgreSQL API smoke tests passed.
```

### Ficheiros atualizados

- `scripts/smoke-test-api.ps1`
- `README.md`
- `LOG.md`

### Trabalho pendente

- tratar `HttpMessageNotReadableException` para JSON malformado;
- corrigir os registos antigos com caracteres corrompidos;
- implementar armazenamento seguro das passwords;
- criar testes Maven com uma instância PostgreSQL dedicada;
- validar o JAR final numa instalação independente.

### Próxima etapa

Rever o diff final, criar o commit da branch
`test/postgresql-resource-crud` e abrir um pull request para `develop`.

## Marco 7 - 22/07/2026 - Tratamento seguro de JSON malformado

### Problema identificado

Os pedidos com JSON malformado não eram tratados pelo `ApiExceptionHandler`.

O Spring devolvia HTTP `400`, mas a resposta padrão podia expor:

- stack trace;
- nomes de classes internas;
- detalhes do parser JSON;
- campos como `trace`, `error`, `exception` e `path`.

### Implementação

Foi adicionado tratamento específico para
`HttpMessageNotReadableException`.

A API passa a devolver uma resposta baseada em `ApiError`:

```json
{
  "timestamp": "...",
  "status": 400,
  "message": "The request body contains invalid JSON.",
  "validationErrors": {}
}
```

Também foram adicionados:

- um teste MockMvc para JSON malformado;
- validação da mensagem e do estado HTTP;
- verificação da ausência de detalhes internos;
- um cenário equivalente no smoke test PostgreSQL;
- limpeza automática dos ficheiros temporários utilizados pelo teste.

### Validação realizada

Foram confirmados:

- `TaskControllerTest`: 3 testes sem falhas;
- suite Maven completa: 19 testes sem falhas ou erros;
- resposta real HTTP `400` através da porta `8080`;
- ausência de `trace`, `error`, `exception` e `path`;
- smoke test completo com PostgreSQL;
- código de saída do smoke test: `0`;
- reposição das contagens iniciais da base de dados.

Mensagem final:

```text
All PostgreSQL API smoke tests passed.
```

### Ficheiros atualizados

- `backend/src/main/java/com/devflowhub/backend/exception/ApiExceptionHandler.java`
- `backend/src/test/java/com/devflowhub/backend/controller/TaskControllerTest.java`
- `scripts/smoke-test-api.ps1`
- `README.md`
- `LOG.md`

### Trabalho pendente

- corrigir os registos antigos com caracteres corrompidos;
- implementar armazenamento seguro das passwords;
- criar testes Maven com uma instância PostgreSQL dedicada;
- concluir a interface Thymeleaf e o frontend React;
- validar o JAR final numa instalação independente.

### Próxima etapa

Rever o diff final, criar o commit da branch
`fix/api-malformed-json-errors` e abrir um pull request para `develop`.

## Marco 8 — Diagnóstico de encoding no PostgreSQL e Windows PowerShell

- A base de dados devflow_hub foi confirmada com server_encoding, client_encoding e encoding da base em UTF8.
- A inspeção hexadecimal confirmou que valores como Gestão, página e Práticas estão armazenados com bytes UTF-8 corretos.
- A resposta JSON bruta da API também apresentou corretamente Módulo, Gestão e outros caracteres portugueses.
- As sequências visíveis como GestÃ£o no psql e no Invoke-RestMethod do Windows PowerShell 5.1 resultam da interpretação incorreta da saída UTF-8 pelo terminal, e não de corrupção dos dados.
- Nenhum UPDATE, alteração de schema ou recriação da base de dados foi necessária.
- Para validação fiável no Windows PowerShell 5.1, a resposta pode ser gravada com curl.exe e lida explicitamente através de ReadAllText(..., UTF8); PowerShell 7 também evita este problema de apresentação.

## Marco 9 — 22/07/2026 — Login e alteração de password

### Objetivo

Foi criado o primeiro fluxo de autenticação do backend sem ativar ainda uma
sessão HTTP ou JWT.

Foram disponibilizados:

```text
POST /api/auth/login
PUT  /api/auth/change-password
```

### Implementação

A autenticação utiliza o `PasswordEncoder` já configurado e compara passwords
através de `matches()`.

Foram adicionados DTOs próprios para:

- pedido de login;
- pedido de alteração de password;
- resposta pública do colaborador autenticado.

A resposta de login contém apenas:

- identificador;
- nome;
- email;
- função;
- estado ativo.

O hash da password nunca é devolvido.

Credenciais inválidas, colaboradores inexistentes e colaboradores inativos
devolvem a mesma mensagem genérica com HTTP `401`:

```text
Invalid email or password.
```

A alteração de password:

- exige a password atual;
- rejeita a reutilização da password atual;
- valida uma nova password entre 8 e 64 caracteres;
- guarda apenas o novo hash.

### Testes

Foram adicionados testes de service e controller para:

- login válido;
- credenciais inválidas;
- resposta sem password;
- HTTP `401` genérico;
- alteração de password;
- rejeição da reutilização da password atual;
- validação do payload;
- resposta HTTP `204`.

Resultado da suite Maven:

```text
Tests run: 32
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

A validação com PostgreSQL real confirmou:

- login com a password inicial;
- alteração da password;
- rejeição da password antiga com HTTP `401`;
- login com a nova password;
- eliminação do colaborador temporário.

O smoke test PostgreSQL foi ampliado para manter estes cenários como regressão.

### Limitação naquele momento

O endpoint de login apenas validava credenciais e devolvia dados públicos. Ainda
não criava sessão, cookie ou token e ainda não protegia os restantes endpoints.

Esta limitação foi resolvida no Marco 10 com a implementação de JWT e Spring Security.

### Ficheiros principais

- `backend/src/main/java/com/devflowhub/backend/controller/AuthController.java`
- `backend/src/main/java/com/devflowhub/backend/service/AuthenticationService.java`
- `backend/src/main/java/com/devflowhub/backend/dto/LoginRequest.java`
- `backend/src/main/java/com/devflowhub/backend/dto/ChangePasswordRequest.java`
- `backend/src/main/java/com/devflowhub/backend/dto/AuthenticatedCollaboratorResponse.java`
- `backend/src/main/java/com/devflowhub/backend/exception/AuthenticationFailedException.java`
- `backend/src/main/java/com/devflowhub/backend/exception/ApiExceptionHandler.java`
- `backend/src/test/java/com/devflowhub/backend/controller/AuthControllerTest.java`
- `backend/src/test/java/com/devflowhub/backend/service/AuthenticationServiceTest.java`
- `scripts/smoke-test-api.ps1`
- `README.md`
- `LOG.md`

### Próxima etapa

Definir e implementar autenticação persistente com sessão HTTP ou JWT e
proteger os endpoints que exigem um colaborador autenticado.

---

## Marco 10 - 23/07/2026 - Autenticação JWT e proteção da API

### Objetivo

Transformar o login já existente num fluxo de autenticação persistente através de JWT e proteger os endpoints da API.

### Funcionalidades implementadas

- configuração do Spring Security como OAuth2 Resource Server;
- geração e assinatura de JSON Web Tokens;
- token do tipo `Bearer` com validade de 900 segundos;
- configuração de `JWT_SECRET` e `JWT_ISSUER` através de variáveis de ambiente;
- `POST /api/auth/login` mantido como endpoint público;
- proteção dos restantes endpoints em `/api/**`;
- identificação do colaborador através da claim `sub`;
- alteração de palavra-passe ligada ao colaborador autenticado;
- rejeição de colaboradores inativos;
- resposta HTTP `401` estruturada para token ausente, inválido ou expirado;
- ampliação dos testes unitários, MockMvc, integração de segurança e smoke tests.

### Validação

Foram confirmados:

- login válido com emissão de JWT;
- rejeição de credenciais inválidas;
- acesso a endpoint protegido com Bearer token válido;
- rejeição de pedidos sem token;
- rejeição de token inválido;
- rejeição de colaborador inativo;
- alteração de palavra-passe utilizando a identidade do token;
- ocultação de passwords e hashes nas respostas.

Resultado da suite:

```text
Tests run: 38
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O smoke test PostgreSQL terminou com:

```text
All PostgreSQL API smoke tests passed.
```

### Ficheiros principais

- `backend/src/main/java/com/devflowhub/backend/config/JwtConfig.java`
- `backend/src/main/java/com/devflowhub/backend/config/JwtProperties.java`
- `backend/src/main/java/com/devflowhub/backend/config/SecurityConfig.java`
- `backend/src/main/java/com/devflowhub/backend/security/ApiAuthenticationEntryPoint.java`
- `backend/src/main/java/com/devflowhub/backend/service/JwtTokenService.java`
- `backend/src/main/java/com/devflowhub/backend/dto/LoginResponse.java`
- testes de autenticação e segurança;
- `scripts/smoke-test-api.ps1`;
- `README.md`.

### Git

- commit principal: `bd6a640`;
- merge em `develop`: `7a6e2c3`;
- pull request: `#11`.

### Estado atual

- a API exige autenticação JWT;
- o login é o único endpoint público funcional em `/api/**`;
- o backend devolve dados suficientes para o frontend criar uma sessão autenticada.

### Próxima etapa

Proteger e validar os campos de auditoria das tarefas antes de iniciar a fundação do frontend React.

---

## Marco 11 - 23/07/2026 - Proteção dos campos de auditoria das tarefas

### Objetivo

Garantir que `createdAt` e `updatedAt` são controlados pela aplicação e persistidos corretamente, sem aceitar valores arbitrários enviados pelo cliente.

### Implementação

- geração automática de `createdAt` e `updatedAt`;
- preservação de `createdAt` durante atualizações;
- atualização automática de `updatedAt`;
- proteção contra valores de auditoria enviados no payload;
- migração datada para bases existentes;
- testes unitários e testes de persistência;
- validação adicional no smoke test PostgreSQL.

### Ficheiros principais

- `backend/src/main/java/com/devflowhub/backend/entity/Task.java`
- `backend/src/main/java/com/devflowhub/backend/service/TaskService.java`
- `backend/src/test/java/com/devflowhub/backend/entity/TaskAuditFieldsTest.java`
- `backend/src/test/java/com/devflowhub/backend/entity/TaskAuditPersistenceTest.java`
- `database/migrations/20260723_add_task_audit_fields.sql`
- `database/migrate_existing_database.sql`
- `scripts/smoke-test-api.ps1`.

### Validação

- criação gera os dois timestamps;
- valores enviados pelo cliente são ignorados;
- atualização preserva `createdAt`;
- atualização renova `updatedAt`;
- leitura devolve os valores persistidos.

### Git

- commit principal: `925bbf8`;
- merge em `develop`: `e9377ef`;
- pull request: `#12`.

### Próxima etapa

Adicionar a fundação React e definir formalmente a arquitetura do frontend.

---

## Marco 12 - 23/07/2026 - Fundação React e decisão de arquitetura

### Objetivo

Criar a aplicação frontend separada do backend e estabelecer React com TypeScript como interface principal do DevFlow Hub.

### Decisão arquitetural

Foi decidido utilizar:

- React;
- TypeScript;
- Vite;
- React Router;
- Fetch API nativa;
- ESLint.

O Spring Boot permanece como API REST responsável por autenticação, regras de negócio, validação e persistência. A decisão completa foi registada em `docs/architecture/frontend-decision.md`.

### Implementação

- inicialização do projeto Vite;
- remoção dos componentes e recursos visuais do template;
- criação das páginas de login, dashboard e recurso não encontrado;
- criação do routing inicial;
- redirecionamento de `/` para `/dashboard`;
- estilos base responsivos;
- preparação de `VITE_API_BASE_URL`;
- configuração dos scripts `dev`, `lint`, `build` e `preview`.

### Ficheiros principais

- `frontend/package.json`
- `frontend/vite.config.ts`
- `frontend/src/main.tsx`
- `frontend/src/routes/AppRoutes.tsx`
- `frontend/src/pages/LoginPage.tsx`
- `frontend/src/pages/DashboardPage.tsx`
- `frontend/src/pages/NotFoundPage.tsx`
- `frontend/src/index.css`
- `docs/architecture/frontend-decision.md`.

### Validação

- instalação das dependências concluída;
- servidor Vite iniciado;
- navegação entre as rotas confirmada;
- lint e build executados com sucesso.

### Git

- `6d6c710`: inicialização React e TypeScript;
- `228798c`: decisão de arquitetura;
- `6d0adf6`: routing inicial;
- merge final em `develop`: `fcefa1c`;
- pull request: `#13`.

### Próxima etapa

Ligar o login React ao endpoint JWT e proteger o dashboard.

---

## Marco 13 - 24/07/2026 - Autenticação JWT no frontend

### Objetivo

Ligar a interface React ao backend autenticado e implementar o ciclo completo de sessão no cliente.

### Funcionalidades implementadas

- cliente HTTP baseado em Fetch API;
- integração com `POST /api/auth/login`;
- tipos TypeScript para autenticação e erros da API;
- `AuthContext`, `AuthProvider` e hook `useAuth`;
- armazenamento da sessão em `sessionStorage`;
- cálculo local de `expiresAt` com base em `expiresIn`;
- rota protegida para `/dashboard`;
- redirecionamento para `/login` sem sessão válida;
- persistência da sessão após atualização da página;
- logout manual;
- expiração automática após 15 minutos;
- proxy Vite de `/api` para `http://localhost:8080`;
- configuração opcional através de `VITE_API_BASE_URL`.

### Validação manual

Foram confirmados:

- mensagem de erro para credenciais inválidas;
- login válido e redirecionamento para o dashboard;
- apresentação dos dados públicos do colaborador;
- persistência após reload;
- logout e proteção da rota;
- expiração automática da sessão;
- `npm run lint` sem erros;
- `npm run build` concluído com sucesso.

### Ficheiros principais

- `frontend/src/api/apiClient.ts`
- `frontend/src/api/authApi.ts`
- `frontend/src/auth/AuthContext.ts`
- `frontend/src/auth/AuthProvider.tsx`
- `frontend/src/auth/authStorage.ts`
- `frontend/src/auth/useAuth.ts`
- `frontend/src/routes/ProtectedRoute.tsx`
- `frontend/src/types/auth.ts`
- páginas, routing, estilos e configuração Vite.

### Git

- commit: `ff73500`;
- merge em `develop`: `d482d42`;
- pull request: `#14`.

### Próxima etapa

Substituir os conteúdos temporários do dashboard por dados reais da API e centralizar o tratamento de HTTP `401`.

---

## Marco 14 - 24/07/2026 - Dashboard autenticado com dados reais

### Objetivo

Consumir o endpoint protegido `GET /api/dashboard`, apresentar os dados reais da aplicação e terminar automaticamente a sessão quando o backend rejeita o token.

### Funcionalidades implementadas

- tipos TypeScript para `DashboardSummary`, tarefas recentes e projetos próximos;
- módulo `dashboardApi`;
- pedido autenticado a `GET /api/dashboard`;
- cartões com totais de colaboradores, projetos, tarefas, programas e tempo registado;
- distribuição de tarefas por estado;
- apresentação das quatro tarefas mais recentes;
- apresentação dos projetos com prazos futuros;
- estados de loading, erro, retry e ausência de dados;
- formatação de estados, datas e duração;
- layout responsivo;
- emissão de evento global quando um pedido autenticado devolve HTTP `401`;
- limpeza da sessão pelo `AuthProvider`;
- redirecionamento automático para `/login`.

### Validação realizada

Foram confirmados no browser:

- carregamento dos valores reais existentes no PostgreSQL;
- 6 colaboradores, 4 projetos, 5 tarefas e 4 programas no conjunto de demonstração;
- contagens das tarefas por estado;
- tarefas recentes com responsável, prioridade, estado e última atividade;
- estado vazio quando não existem projetos com prazos futuros;
- logout normal;
- substituição manual do token por um valor inválido;
- resposta HTTP `401` do backend;
- remoção imediata da sessão;
- redirecionamento para `/login`;
- novo login válido após o teste.

Validação técnica:

```text
npm run lint   -> sucesso
npm run build  -> sucesso
```

O build Vite transformou 34 módulos e terminou sem erros TypeScript.

### Ficheiros atualizados

- `frontend/src/api/apiClient.ts`
- `frontend/src/api/dashboardApi.ts`
- `frontend/src/auth/AuthProvider.tsx`
- `frontend/src/index.css`
- `frontend/src/pages/DashboardPage.tsx`
- `frontend/src/types/dashboard.ts`
- `README.md`
- `LOG.md`
- `frontend/README.md`
- `docs/architecture/frontend-decision.md`
- `frontend/.env.example`.

### Estado antes do commit

- branch: `feature/frontend-authenticated-dashboard`;
- código e documentação revistos;
- lint e build aprovados;
- teste manual de HTTP `401` aprovado;
- estatísticas finais do Git ainda pendentes até à criação do commit.

### Trabalho pendente

- implementar as páginas detalhadas dos recursos;
- adicionar operações de criação e edição no frontend;
- ligar os controlos do temporizador;
- adicionar testes automatizados do frontend;
- rever a estratégia de armazenamento do token antes de produção.

### Próxima etapa

Rever o diff staged, criar o commit da branch e abrir um pull request para `develop`.

---

## Marco 15 - 24/07/2026 - Lista autenticada de projetos

### Objetivo

Criar a primeira página autenticada de consulta de projetos no frontend React e estabelecer uma navegação reutilizável para as futuras áreas da aplicação.

### Funcionalidades implementadas

- criação da rota protegida `/projects`;
- criação de tipos TypeScript para projetos e colaboradores;
- integração autenticada com `GET /api/projects`;
- integração autenticada com `GET /api/collaborators`;
- carregamento paralelo dos projetos e colaboradores;
- associação do `managerId` ao nome do respetivo gestor;
- apresentação dos projetos através de cartões;
- apresentação do identificador, nome, descrição, estado, gestor e datas;
- indicação de valores não definidos;
- estados de loading, erro, retry e lista vazia;
- cabeçalho autenticado reutilizável;
- navegação entre Dashboard e Projects;
- logout disponível nas páginas autenticadas;
- preparação do cliente de API para a futura página `/projects/:id`;
- layout responsivo para desktop e dispositivos móveis.

### Decisões de implementação

Foi criado o componente reutilizável `AppHeader` para evitar a repetição da identidade do utilizador, da navegação e da operação de logout em cada página autenticada.

Os projetos e colaboradores são carregados em paralelo através de `Promise.all`. O frontend constrói depois um mapa entre o identificador do colaborador e o respetivo nome, permitindo apresentar o gestor de cada projeto sem alterar o contrato atual do backend.

A futura página de detalhe do projeto poderá utilizar `getProjectById` e incluir as tarefas e a documentação associadas ao projeto.

### Ficheiros principais

- `frontend/src/api/collaboratorsApi.ts`
- `frontend/src/api/projectsApi.ts`
- `frontend/src/components/AppHeader.tsx`
- `frontend/src/pages/ProjectsPage.tsx`
- `frontend/src/types/collaborator.ts`
- `frontend/src/types/project.ts`
- `frontend/src/pages/DashboardPage.tsx`
- `frontend/src/routes/AppRoutes.tsx`
- `frontend/src/index.css`
- `README.md`
- `frontend/README.md`
- `LOG.md`

### Validação realizada

Foram confirmados:

- carregamento dos quatro projetos existentes no PostgreSQL;
- navegação entre `/dashboard` e `/projects`;
- atualização direta de `/projects` com preservação da sessão;
- apresentação dos estados, descrições, gestores e datas;
- apresentação de `Not assigned` quando não existe gestor;
- logout a partir da página de projetos;
- ausência de scroll horizontal;
- layout desktop validado a 100% de zoom;
- layout móvel validado numa viewport de `390 × 844`;
- cartões apresentados numa única coluna em dispositivos móveis;
- textos, badges e datas mantidos dentro dos respetivos cartões.

### Validação técnica

- `npm run lint`: sucesso;
- `npm run build`: sucesso;
- 38 módulos transformados pelo Vite;
- build concluído sem erros TypeScript.

### Trabalho pendente

- implementar a lista autenticada de tarefas;
- criar a página de detalhe de projeto;
- apresentar as tarefas associadas ao projeto;
- implementar documentação de projeto e de tarefa;
- adicionar operações de criação e edição;
- adicionar testes automatizados do frontend.

### Próxima etapa

Rever o diff final, criar o commit da branch `feature/frontend-projects-list` e abrir um pull request para `develop`.

---

## Marco 16 - 24/07/2026 - Lista autenticada de tarefas

### Objetivo

Criar uma página autenticada para consulta das tarefas existentes, apresentando as relações com projetos e colaboradores e mantendo o padrão visual e técnico estabelecido pelas páginas anteriores.

### Funcionalidades implementadas

- criação da rota protegida `/tasks`;
- criação dos tipos TypeScript `TaskStatus`, `TaskPriority`, `Task` e `TaskListItem`;
- integração autenticada com `GET /api/tasks`;
- preparação do cliente de API para `GET /api/tasks/{id}`;
- carregamento paralelo de tarefas, projetos e colaboradores;
- associação do `projectId` ao nome do respetivo projeto;
- associação do `assigneeId` ao nome do respetivo responsável;
- apresentação do identificador, título, descrição, estado e prioridade;
- apresentação do projeto e responsável associados;
- apresentação do tempo registado em segundos, minutos ou horas;
- apresentação das datas de criação e última atualização;
- indicação de tarefas sem projeto ou responsável;
- indicação do número total de tarefas e de temporizadores ativos;
- preparação da atualização visual do tempo quando existir um temporizador ativo;
- estados de loading, erro, retry e lista vazia;
- inclusão de Tasks na navegação autenticada;
- layout responsivo para desktop e dispositivos móveis.

### Decisões de implementação

A página carrega tarefas, projetos e colaboradores através de `Promise.all`. Depois são construídos mapas entre identificadores e nomes, permitindo apresentar relações legíveis sem alterar os contratos atuais do backend.

O campo `totalTimeSeconds` é apresentado como Tracked time e representa o tempo já registado para cada tarefa.

Quando `timerActive` estiver ativo e existir `timerStartedAt`, o frontend está preparado para somar visualmente a sessão atual ao tempo acumulado e atualizar o valor a cada segundo. Esta lógica não altera o valor persistido pelo backend.

Os controlos para iniciar, pausar, retomar ou concluir tarefas não foram incluídos nesta etapa. Essas operações serão implementadas separadamente devido ao seu impacto no estado persistido.

### Ficheiros principais

- `frontend/src/api/tasksApi.ts`
- `frontend/src/components/AppHeader.tsx`
- `frontend/src/index.css`
- `frontend/src/pages/TasksPage.tsx`
- `frontend/src/routes/AppRoutes.tsx`
- `frontend/src/types/task.ts`
- `README.md`
- `frontend/README.md`
- `LOG.md`

### Validação realizada

Foram confirmados:

- carregamento das cinco tarefas existentes no PostgreSQL;
- navegação entre `/dashboard`, `/projects` e `/tasks`;
- opção Tasks apresentada como ativa;
- associação correta das tarefas aos projetos e responsáveis;
- apresentação de `No project` quando não existe projeto;
- apresentação de `Not assigned` quando não existe responsável;
- apresentação dos estados e prioridades através de badges;
- apresentação do tempo registado;
- apresentação das datas de criação e última atualização;
- ausência de scroll horizontal;
- layout desktop validado a 100% de zoom;
- layout móvel validado numa viewport de `390 × 844`;
- cartões apresentados numa única coluna em dispositivos móveis;
- navegação e logout mantidos acessíveis no layout móvel.

O conjunto de demonstração possuía zero temporizadores ativos. Por esse motivo, a atualização visual por segundo não foi exercitada manualmente nesta validação.

### Validação técnica

- `npm run lint`: sucesso;
- `npm run build`: sucesso;
- 40 módulos transformados pelo Vite;
- build concluído sem erros TypeScript;
- CSS de produção com 10,41 kB;
- JavaScript de produção com 254,26 kB;
- `git diff --check` concluído sem erros.

### Trabalho pendente

- criar a página de detalhe de projeto;
- criar a página de detalhe de tarefa;
- apresentar as tarefas associadas dentro do detalhe do projeto;
- implementar os controlos do temporizador;
- implementar documentação de projeto e de tarefa;
- adicionar operações de criação, edição e eliminação;
- adicionar testes automatizados do frontend.

### Próxima etapa

Rever o diff completo, criar o commit da branch `feature/frontend-tasks-list` e abrir um pull request para `develop`.
---

## Marco 17 - 24/07/2026 - Detalhe autenticado de projeto

### Objetivo

Criar uma página autenticada de detalhe de projeto, permitir a navegação a partir da lista de projetos e apresentar as tarefas associadas ao projeto selecionado.

### Funcionalidades implementadas

- criação da rota protegida `/projects/:projectId`;
- criação da página `ProjectDetailPage`;
- ligação de cada cartão da lista de projetos ao respetivo detalhe;
- integração com `GET /api/projects/{id}`;
- carregamento dos colaboradores através de `GET /api/collaborators`;
- carregamento das tarefas através de `GET /api/tasks`;
- associação do `managerId` ao nome do gestor;
- associação do `assigneeId` ao nome do responsável;
- filtragem das tarefas através do respetivo `projectId`;
- apresentação dos dados principais do projeto;
- apresentação das tarefas associadas;
- estado vazio para projetos sem tarefas;
- tratamento de identificadores inválidos e projetos inexistentes;
- ligação para regressar à lista de projetos;
- manutenção da navegação e do logout;
- layout responsivo para desktop e dispositivos móveis.

### Decisões de implementação

O backend disponibiliza `GET /api/projects/{id}`, mas ainda não possui um endpoint específico para obter apenas as tarefas de um projeto.

A página carrega todas as tarefas através de `GET /api/tasks` e filtra no frontend os registos cujo `projectId` corresponde ao projeto.

Esta solução é adequada ao volume atual de dados. Num cenário com maior volume, deverá ser considerado um endpoint como `GET /api/projects/{id}/tasks`.

Os dados do projeto, colaboradores e tarefas são carregados em paralelo através de `Promise.all`.

O campo `totalTimeSeconds` continua a ser apresentado como Tracked time e representa apenas o tempo já registado.

### Ficheiros principais

- `frontend/src/index.css`
- `frontend/src/pages/ProjectDetailPage.tsx`
- `frontend/src/pages/ProjectsPage.tsx`
- `frontend/src/routes/AppRoutes.tsx`
- `README.md`
- `frontend/README.md`
- `LOG.md`

### Validação realizada

Foram confirmados:

- acesso ao detalhe através de View project;
- rota direta `/projects/:projectId`;
- projetos com zero, uma e duas tarefas;
- filtragem correta das tarefas por `projectId`;
- associação correta dos responsáveis;
- apresentação de estado, prioridade e tempo registado;
- apresentação de `Not assigned` e `Not set`;
- estado vazio No associated tasks;
- `/projects/abc` tratado como Project not found;
- `/projects/999999` tratado como Project not found;
- atualização direta com preservação da sessão;
- navegação entre Dashboard, Projects e Tasks;
- ligação Back to projects;
- logout acessível;
- ausência de sobreposição e overflow horizontal;
- layout desktop validado a 100% de zoom;
- layout móvel validado numa viewport de `390 × 844`.

### Validação técnica

- `npm run lint`: sucesso;
- `npm run build`: sucesso;
- 41 módulos transformados pelo Vite;
- build concluído sem erros TypeScript;
- CSS de produção com 13,48 kB;
- JavaScript de produção com 260,44 kB;
- `git diff --check` concluído sem erros.

### Trabalho pendente

- criar a página de detalhe de tarefa;
- implementar os controlos do temporizador;
- implementar documentação de projeto e de tarefa;
- adicionar operações de criação, edição e eliminação;
- avaliar um endpoint backend específico para tarefas de um projeto;
- adicionar testes automatizados do frontend.

### Próxima etapa

Rever o diff completo, criar o commit da branch `feature/frontend-project-detail` e abrir um pull request para `develop`.
---

## Marco 18 - 24/07/2026 - Detalhe de tarefa e controlos do temporizador

### Objetivo

Criar uma página autenticada de detalhe de tarefa e disponibilizar o ciclo completo do temporizador através da interface React.

### Funcionalidades implementadas

- criação da rota protegida `/tasks/:taskId`;
- criação da página `TaskDetailPage`;
- ligação View task na lista geral de tarefas;
- ligação View task nas tarefas associadas ao projeto;
- integração com `GET /api/tasks/{id}`;
- carregamento dos projetos e colaboradores;
- resolução do nome do projeto;
- resolução do nome do responsável;
- apresentação do estado e prioridade;
- apresentação da descrição;
- apresentação do tempo acumulado;
- apresentação do estado do temporizador;
- apresentação da data de início do temporizador;
- apresentação das datas de criação e atualização;
- ligação do projeto ao respetivo detalhe;
- tratamento de tarefas sem projeto ou responsável;
- tratamento de identificadores inválidos;
- tratamento de tarefas inexistentes;
- ligação Back to tasks;
- integração com o endpoint de início do temporizador;
- integração com o endpoint de pausa;
- integração com o endpoint de retoma;
- integração com o endpoint de conclusão;
- atualização visual do tempo a cada segundo;
- bloqueio dos botões durante os pedidos;
- mensagens de processamento;
- mensagens de sucesso e erro;
- confirmação antes da conclusão;
- acumulação da sessão ativa ao concluir;
- remoção dos controlos após a conclusão;
- indicação de que uma tarefa concluída não pode reiniciar o temporizador;
- layout responsivo para desktop e dispositivos móveis.

### Decisões de implementação

A tarefa, os projetos e os colaboradores são carregados em paralelo através de `Promise.all`.

Enquanto o temporizador está ativo, o frontend calcula o tempo apresentado através da soma de `totalTimeSeconds` com o tempo decorrido desde `timerStartedAt`.

O valor persistido continua a ser controlado pelo backend. O frontend utiliza a atualização por segundo apenas para apresentar a sessão ativa em tempo real.

As operações do temporizador reutilizam uma função interna do cliente de API para os endpoints:

```text
POST /api/tasks/{id}/start-timer
POST /api/tasks/{id}/pause-timer
POST /api/tasks/{id}/resume-timer
POST /api/tasks/{id}/complete
```

A conclusão utiliza uma confirmação explícita porque uma tarefa concluída não pode voltar a iniciar ou retomar o temporizador.

### Ficheiros principais

- `frontend/src/api/tasksApi.ts`
- `frontend/src/index.css`
- `frontend/src/pages/TaskDetailPage.tsx`
- `frontend/src/pages/TasksPage.tsx`
- `frontend/src/pages/ProjectDetailPage.tsx`
- `frontend/src/routes/AppRoutes.tsx`
- `README.md`
- `frontend/README.md`
- `LOG.md`

### Validação realizada

Foram confirmados:

- ligação View task na lista de tarefas;
- ligação View task no detalhe do projeto;
- carregamento direto de `/tasks/:taskId`;
- apresentação correta das tarefas existentes;
- tarefa com projeto e responsável;
- tarefa sem projeto e sem responsável;
- estado Pending;
- estado In Progress;
- estado Completed;
- estado Not running;
- estado Running;
- apresentação do tempo acumulado;
- ligação para o detalhe do projeto;
- `/tasks/abc` tratado como Task not found;
- `/tasks/999999` tratado como Task not found;
- atualização da página com preservação da sessão;
- navegação autenticada mantida;
- layout desktop validado a 100% de zoom;
- layout móvel validado numa viewport de `390 × 844`;
- ausência de sobreposição dos elementos;
- ausência de scroll horizontal;
- botões adaptados à largura do ecrã móvel.

### Validação do temporizador

Foi criada a tarefa temporária `Timer workflow validation` para exercer o ciclo completo.

Foram confirmados:

1. início com estado Pending, tempo `0s` e botão Start timer;
2. alteração para In Progress e Running depois do início;
3. atualização visual do tempo a cada segundo;
4. apresentação do botão Pause timer durante a sessão;
5. pausa com persistência do tempo acumulado;
6. alteração do botão para Resume timer;
7. preservação do tempo após atualização da página;
8. retoma a partir do tempo anteriormente registado;
9. confirmação antes da conclusão;
10. acumulação da sessão ativa no tempo total;
11. alteração final para Completed;
12. remoção dos botões depois da conclusão;
13. bloqueio da possibilidade de reiniciar o temporizador;
14. apresentação das mensagens de sucesso;
15. eliminação da tarefa temporária após a validação;
16. regresso da lista original para cinco tarefas e zero temporizadores ativos.

### Validação técnica

- `npm run lint`: sucesso;
- `npm run build`: sucesso;
- 42 módulos transformados pelo Vite;
- build concluído sem erros TypeScript;
- CSS de produção com 17,38 kB;
- JavaScript de produção com 268,44 kB;
- `git diff --check` concluído sem erros.

### Trabalho pendente

- implementar documentação e anexos de projetos e tarefas;
- adicionar operações de criação e edição através do frontend;
- adicionar testes automatizados do frontend;
- rever a estratégia de armazenamento do token antes de produção;
- avaliar tratamento adicional de concorrência entre várias janelas.

### Próxima etapa

Rever o diff completo, criar o commit da branch `feature/frontend-task-detail` e abrir um pull request para `develop`.

---

## Marco 19 - 25/07/2026 a 26/07/2026 - Fundação de documentos e metadata de anexos

### Objetivo

Adicionar ao domínio uma base persistente para documentação associada a projetos e tarefas, preparando a futura integração de ficheiros sem misturar metadata com conteúdo binário.

### Funcionalidades implementadas

- criação da entidade `Document`;
- criação da entidade `Attachment`;
- associação de cada documento a exatamente um projeto ou uma tarefa;
- associação dos attachments ao respetivo documento;
- inclusão de campos de auditoria e metadata de ficheiros;
- criação dos repositories e services de documentos e attachments;
- criação do `DocumentController`;
- criação do `AttachmentController`;
- implementação do CRUD de documentos;
- implementação das consultas de documentos por projeto e por tarefa;
- implementação das consultas de metadata de attachments;
- criação do DTO `AttachmentMetadataResponse`;
- adição das alterações necessárias aos scripts PostgreSQL e às migrações;
- criação de testes de controller, service, persistência e campos de auditoria.

### Endpoints adicionados

```text
GET    /api/documents/{id}
GET    /api/projects/{projectId}/documents
GET    /api/tasks/{taskId}/documents
POST   /api/documents
PUT    /api/documents/{id}
DELETE /api/documents/{id}
GET    /api/attachments/{id}
GET    /api/documents/{documentId}/attachments
```

### Decisões de implementação

O conteúdo dos ficheiros não foi guardado diretamente na base de dados. A tabela de attachments conserva apenas a metadata necessária para relacionar o registo com um provider de object storage.

A API desta fase disponibiliza apenas metadata de attachments. Upload, download e eliminação de conteúdo ficaram deliberadamente fora do escopo até existir uma camada de armazenamento e autorização adequada.

### Validação

- persistência de documentos e attachments validada com H2;
- constraints de associação validadas;
- campos de auditoria validados;
- testes de services e controllers concluídos sem falhas;
- PR #20 integrado em `develop`.

### Trabalho pendente

- implementar object storage;
- adicionar upload e download HTTP;
- registar o autor do documento e o utilizador que fez o upload;
- aplicar autorização através do projeto ou tarefa parent;
- integrar documentos e attachments no frontend.

### Próxima etapa

Melhorar a instalação do projeto, automatizar a validação no GitHub e preparar uma abstração segura para armazenamento de ficheiros.

---

## Marco 20 - 27/07/2026 - Configuração, CI e testes automatizados do frontend

### Objetivo

Melhorar a reprodutibilidade do projeto, remover dependências não utilizadas, automatizar a validação dos pull requests e criar uma primeira suite de testes do frontend.

### Funcionalidades e melhorias implementadas

- atualização da documentação das variáveis de ambiente;
- esclarecimento da arquitetura React e Spring Boot;
- remoção da dependência Thymeleaf, que já não fazia parte da arquitetura atual;
- criação do workflow `.github/workflows/build-validation.yml`;
- validação automática de pull requests e pushes para `develop`;
- configuração do Java 21 e cache Maven no job do backend;
- execução de `clean verify` no backend;
- configuração do Node.js 22 e instalação com `npm ci` no frontend;
- execução de lint, testes e build no frontend;
- atualização do React Router para `8.3.0`;
- criação da infraestrutura Vitest, React Testing Library e jsdom;
- criação de testes para armazenamento da autenticação;
- criação de testes para `ProtectedRoute`;
- criação de testes para a configuração principal das rotas.

### Testes do frontend adicionados

```text
frontend/src/auth/authStorage.test.ts
frontend/src/routes/ProtectedRoute.test.tsx
frontend/src/routes/AppRoutes.test.tsx
```

### Validação

- `npm run lint`: sucesso;
- `npm test`: 16 testes aprovados em 3 ficheiros;
- `npm run build`: sucesso;
- workflow de GitHub Actions validado;
- PRs #22, #23, #24, #28 e #29 integrados em `develop`.

### Decisões de implementação

O workflow utiliza `npm ci` para respeitar exatamente o `package-lock.json` e reduzir diferenças entre ambientes.

Os testes iniciais concentram-se nas áreas com maior impacto transversal: persistência da sessão, proteção de rotas, redirecionamentos e configuração das rotas principais.

### Trabalho pendente

- aumentar a cobertura das páginas e clientes de API;
- adicionar testes das ações do temporizador no frontend;
- adicionar testes com PostgreSQL real no pipeline;
- validar artefactos finais numa instalação independente.

### Próxima etapa

Criar a fundação persistente do controlo de acesso a projetos e a abstração do object storage.

---

## Marco 21 - 27/07/2026 - Fundação de memberships e object storage

### Objetivo

Criar as duas fundações necessárias para a próxima fase do sistema: memberships de projeto para autorização por recurso e uma abstração segura para armazenamento de ficheiros.

### Fundação de memberships

- criação da entidade `ProjectMembership`;
- criação dos papéis `OWNER`, `MANAGER`, `CONTRIBUTOR` e `VIEWER`;
- criação dos estados `ACTIVE` e `INACTIVE`;
- ligação entre projeto e colaborador através de identificadores persistidos;
- adição de `@Version` para concorrência otimista;
- adição de timestamps de criação e atualização;
- criação de uma restrição única para o par projeto e colaborador;
- criação do repository de memberships;
- atualização dos scripts e migrações PostgreSQL;
- criação de testes de persistência e constraints.

### Fundação de object storage

- criação do contrato `ObjectStorage`;
- criação dos modelos de pedidos e respostas do storage;
- implementação do provider local;
- validação e normalização das chaves de objetos;
- proteção contra traversal de diretórios;
- proteção contra symlinks;
- escrita através de ficheiro temporário antes da substituição final;
- criação de testes de contrato reutilizáveis;
- validação de escrita, leitura, substituição, eliminação e falhas de streams.

### Decisões de implementação

A membership passou a ser preparada como fonte de verdade para acesso ao projeto. O campo profissional `Collaborator.role` não deve ser reutilizado como papel de autorização.

O object storage foi definido através de uma interface para permitir a substituição futura do provider local por um serviço externo sem alterar os services de domínio.

### Validação

- unicidade de memberships validada na base de dados;
- concorrência otimista preparada através do campo `version`;
- testes de contrato do provider local aprovados;
- proteção de caminhos e symlinks validada;
- PRs #30 e #31 integrados em `develop`.

### Trabalho pendente

- resolver o colaborador atual a partir do JWT;
- definir a matriz de permissões;
- aplicar memberships aos endpoints de projetos e tarefas;
- configurar o provider de storage em runtime;
- ligar attachments ao object storage.

### Próxima etapa

Criar uma fundação central de autorização e documentar formalmente a matriz de acesso.

---

## Marco 22 - 28/07/2026 - Arquitetura central de autorização de projetos

### Objetivo

Separar autenticação de autorização e criar uma base central para verificar o acesso do colaborador autenticado a cada projeto.

### Funcionalidades implementadas

- criação do `CurrentCollaboratorResolver`;
- resolução do colaborador através da claim JWT `sub`;
- rejeição de colaboradores inexistentes ou inativos;
- criação do enum `ProjectPermission`;
- criação do `ProjectAccessService`;
- verificação de memberships ativas;
- distinção entre recursos ocultos e operações não permitidas;
- introdução de `ProjectAccessDeniedException`;
- criação de testes unitários para o resolver, permissões e serviço de acesso;
- correção da resolução do identificador do colaborador autenticado;
- configuração do Mockito como Java agent para Java 21;
- correção de um teste do object storage para não depender do tamanho do buffer.

### Documentação criada

```text
docs/architecture/project-access-control.md
docs/architecture/project-access-permission-matrix.md
docs/architecture/project-access-endpoint-inventory.md
```

### Semântica HTTP definida

- `401 Unauthorized` para autenticação ausente, inválida ou associada a colaborador inativo;
- `404 Not Found` para recurso inexistente ou oculto ao utilizador;
- `403 Forbidden` quando o utilizador pode conhecer o recurso, mas o papel não permite a operação.

### Decisões de arquitetura

A autorização é aplicada nos services e não apenas nos controllers. Desta forma, chamadas futuras provenientes de jobs, integrações ou outros controllers continuam protegidas.

As operações que movem um recurso devem autorizar primeiro o parent atual e depois o parent de destino. Nenhuma mutação deve ser persistida antes de todas as verificações passarem.

### Validação

- testes do `CurrentCollaboratorResolver`: aprovados;
- testes do `ProjectPermission`: aprovados;
- testes do `ProjectAccessService`: aprovados;
- documentação revista e integrada;
- PRs #32, #33, #34 e #35 integrados em `develop`.

### Trabalho pendente

- aplicar a autorização às operações existentes de projetos;
- criar automaticamente a membership do criador;
- aplicar a mesma arquitetura às tarefas;
- definir políticas para documentos, attachments e recursos globais.

### Próxima etapa

Aplicar a autorização central aos endpoints existentes e configurar o object storage para execução real.

---

## Marco 23 - 28/07/2026 - Enforcement de projetos, configuração do storage e ownership automático

### Objetivo

Transformar as fundações anteriores em comportamento real da API, protegendo os projetos existentes, configurando o provider de storage e estabelecendo ownership na criação de projetos.

### Controlo de acesso a projetos

- listagem de projetos filtrada por memberships ativas;
- consulta de projeto protegida por `VIEW_PROJECT`;
- atualização protegida por `MANAGE_PROJECT`;
- eliminação protegida por `DELETE_PROJECT`;
- ocultação consistente de projetos sem membership através de `404`;
- resposta `403` quando existe membership mas o papel é insuficiente;
- criação de queries de repository específicas para acesso e contagem;
- atualização dos testes de service e repository.

### Configuração do object storage

- criação de propriedades de configuração para o provider;
- suporte inicial para o provider `local`;
- configuração do diretório raiz através de variável de ambiente;
- falha rápida quando o provider ou diretório obrigatório é inválido;
- definição dos defaults de desenvolvimento;
- criação de testes da configuração do Spring.

### Ownership automático na criação

- qualquer colaborador autenticado e ativo pode criar um projeto;
- o criador torna-se manager inicial;
- o criador recebe uma membership `OWNER` ativa;
- o projeto e a membership são criados na mesma transação;
- um `managerId` diferente do criador é rejeitado;
- uma falha ao criar a membership provoca rollback do projeto.

### Validação

- queries de projetos acessíveis validadas com persistência H2;
- matriz de permissões exercida em testes;
- configuração do storage validada;
- criação transacional e rollback validados;
- PRs #36, #37 e #38 integrados em `develop`.

### Trabalho pendente

- proteger tarefas e os respetivos temporizadores;
- criar endpoints de gestão de memberships;
- validar a consistência futura entre `managerId` e memberships;
- aplicar autorização a documentos e attachments.

### Próxima etapa

Implementar o controlo de acesso de tarefas, incluindo tarefas independentes, reassignment e movimentação entre projetos.

---

## Marco 24 - 29/07/2026 - Controlo de acesso de tarefas

### Objetivo

Aplicar autorização completa às tarefas, impedindo acesso por manipulação de identificadores e preservando as regras pessoais do temporizador.

### Funcionalidades implementadas

- criação do `TaskAccessService`;
- listagem de tarefas filtrada no repository;
- consulta de tarefas protegida;
- criação de tarefas de projeto sujeita a permissão de contribuição;
- criação de tarefas independentes limitada ao próprio colaborador;
- tarefas independentes privadas do assignee;
- atualização e eliminação sujeitas ao papel e ownership aplicáveis;
- autorização do parent atual antes da validação do parent de destino;
- autorização separada do projeto de destino;
- validação de memberships do assignee no projeto de destino;
- restrições de reassignment para contributors;
- ações de início, pausa, retoma e conclusão limitadas ao assignee;
- consulta do tempo e estado do temporizador protegida pela visibilidade da tarefa;
- ocultação de tarefas inacessíveis através de `404`;
- resposta `403` para ações conhecidas mas não permitidas;
- proteção contra alterações parciais antes da conclusão das verificações.

### Testes adicionados e atualizados

- 38 testes específicos de `TaskAccessService`;
- testes de queries de acesso no `TaskRepository`;
- atualização extensa dos testes de `TaskService`;
- atualização dos testes do `TaskController`;
- atualização dos testes HTTP de segurança.

### Validação

Na baseline executada após o merge:

```text
Tests run: 171
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

O frontend foi igualmente validado:

```text
Test Files: 3 passed
Tests: 16 passed
Lint: aprovado
Build: aprovado
```

### Review e merge

- PR #39 aprovado;
- review confirmou a distinção entre `404` e `403`;
- review confirmou a ordem das verificações na movimentação entre projetos;
- review confirmou a validação do assignee no destino;
- PR #39 integrado em `develop`;
- branch local e remota da funcionalidade eliminadas depois do merge.

### Follow-up identificado

Foi recomendada cobertura HTTP de integração adicional para movimentação de tarefas entre projetos, incluindo validação do estado persistido depois de operações recusadas.

### Próxima etapa

Implementar a gestão de memberships dos projetos sem misturar ainda frontend, documentos ou autorização administrativa global.

---

## Marco 25 - 29/07/2026 - Gestão de memberships dos projetos. PR #40

### Estado

Implementação concluída na branch `feature/project-membership-management` e publicada no commit:

```text
84c391c feat: add project membership management
```

O PR #40 foi aprovado e integrado em `develop` depois de review técnica.

### Objetivo

Disponibilizar uma API segura para consultar e gerir membros dos projetos, protegendo ownership, hierarquia de papéis, consistência do manager e concorrência.

### Endpoints implementados

```text
GET    /api/projects/{projectId}/members
POST   /api/projects/{projectId}/members
PATCH  /api/projects/{projectId}/members/{collaboratorId}
DELETE /api/projects/{projectId}/members/{collaboratorId}
```

### Funcionalidades implementadas

- criação do `ProjectMembershipController`;
- criação do `ProjectMembershipService`;
- criação de DTOs de pedido e resposta;
- listagem de memberships do projeto;
- adição de colaboradores ativos;
- alteração de papéis;
- remoção lógica através do estado `INACTIVE`;
- reativação da membership existente ao adicionar novamente um antigo membro;
- rejeição de memberships ativas duplicadas;
- proteção dos endpoints genéricos contra criação, alteração ou remoção de `OWNER`;
- autoridade de `MANAGER` limitada a `CONTRIBUTOR` e `VIEWER`;
- prevenção de privilege escalation;
- proteção do colaborador definido em `project.managerId` contra remoção ou despromoção incompatível;
- validação de que um novo manager é colaborador ativo e membro ativo com papel `OWNER` ou `MANAGER`;
- suporte de concorrência otimista através do campo `version`;
- resposta `409 Conflict` para dados desatualizados e memberships duplicadas;
- tratamento neutro de conflitos de optimistic locking.

### Validação

A suite final foi executada depois do último refinamento e antes do merge:

```text
Tests run: 205
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

Foram incluídos:

- 19 testes do `ProjectMembershipService`;
- 9 testes do `ProjectMembershipController`;
- novos testes de consistência no `ProjectService`;
- novos testes do tratamento de conflitos;
- atualização dos testes de permissões.

### Decisões de implementação

A remoção é lógica para preservar o histórico da relação entre projeto e colaborador.

A transferência de ownership não é realizada pelos endpoints genéricos. Deve existir posteriormente uma operação explícita e transacional com regras próprias.

A interface React de gestão de membros ficou fora deste PR para manter o escopo concentrado no backend e permitir uma review aprofundada das regras de segurança.

### Review e merge

A review técnica confirmou:

- matriz de permissões consistente com o princípio de least privilege;
- papéis definidos com restrição clara de poderes administrativos;
- proteção contra privilege escalation, mantendo a transferência de ownership fora dos endpoints genéricos;
- gestão sólida de membros inativos através de `INACTIVE` e reativação da membership existente;
- utilização adequada de `409 Conflict` para duplicados e concorrência;
- validação consistente de `project.managerId` contra membros ativos e elegíveis.

Não foram solicitadas alterações antes do merge. O PR #40 foi integrado em `develop`.

### Trabalho pendente

- criar a interface frontend de gestão de membros;
- criar a transferência explícita e transacional de ownership;
- adicionar testes HTTP de integração da autorização e concorrência;
- eliminar a branch funcional depois de sincronizar o repositório local.

### Próxima etapa

Avançar numa branch baseada no `develop` atualizado com os testes HTTP de integração recomendados para a movimentação de tarefas entre projetos.

---

## Marco 26 - 29/07/2026 - Testes HTTP de movimentação de tarefas entre projetos

### Estado

Implementação concluída na branch `test/task-move-authorization-integration` e registada no commit:

```text
94d9177 test: cover task moves between projects
```

Esta entrega fecha o follow-up técnico identificado durante a review do PR #39.

### Objetivo

Validar o fluxo completo de movimentação de tarefas entre projetos através da API, incluindo controller, service, autorização, tratamento de erros e estado persistido.

### Cobertura adicionada

Foram adicionados 10 testes HTTP de integração para confirmar:

- projeto atual inexistente ou oculto devolve `404`;
- a tarefa e o projeto atual são autorizados antes da validação do destino;
- projeto de destino inexistente devolve `404`;
- projeto de destino existente, mas invisível, devolve `404`;
- membership `VIEWER` no destino devolve `403`;
- assignee sem membership ativa no destino devolve `400`;
- assignee com membership inativa no destino devolve `400`;
- `OWNER` pode mover a tarefa com um assignee válido;
- `OWNER` pode mover a tarefa sem assignee;
- `CONTRIBUTOR` que move a própria tarefa sem assignee permanece associado de forma coerente com as regras atuais.

Nos cenários recusados, os testes voltam a consultar a base de dados e confirmam que o projeto e o assignee da tarefa não foram alterados parcialmente.

### Validação direcionada

```text
Tests run: 10
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Validação completa

```text
Tests run: 215
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Impacto

- nenhum código de produção foi alterado;
- a matriz de autorização existente foi validada através do fluxo HTTP completo;
- a ordem das verificações e a ausência de alterações parciais ficaram protegidas contra regressões;
- o total da suite backend passou de 205 para 215 testes.

### Próxima etapa

O follow-up foi integrado em `develop`. A etapa seguinte avançou para a operação explícita e transacional de transferência de ownership.

---

## Marco 27 - 29/07/2026 - Transferência explícita e transacional de ownership

### Estado

Implementação concluída na branch `feature/project-ownership-transfer` e registada no commit:

```text
731e311 feat: add project ownership transfer
```

### Objetivo

Criar uma operação explícita, autorizada e transacional para transferir a propriedade de um projeto sem permitir que os endpoints genéricos de memberships criem, alterem ou removam o papel `OWNER`.

### Contrato da API

Foi adicionado o endpoint:

```text
POST /api/projects/{projectId}/ownership-transfer
```

O pedido inclui:

- `newOwnerCollaboratorId`;
- `currentOwnerMembershipVersion`;
- `newOwnerMembershipVersion`.

As versões das duas memberships permitem detetar pedidos baseados em estado desatualizado antes de aplicar alterações.

### Regras de autorização e domínio

- apenas o `OWNER` atual pode iniciar a transferência;
- não é permitido transferir ownership para o próprio utilizador;
- o destinatário deve ser um colaborador ativo;
- o destinatário deve possuir membership `ACTIVE` no projeto;
- deve existir exatamente um `OWNER` ativo antes da operação;
- o novo proprietário passa a `OWNER`;
- o proprietário anterior passa a `MANAGER`;
- `project.managerId` passa para o novo proprietário;
- `MANAGER`, `CONTRIBUTOR` e `VIEWER` recebem `403`;
- projetos inexistentes ou ocultos devolvem `404`;
- versões desatualizadas e conflitos de optimistic locking devolvem `409 Conflict`.

### Atomicidade e concorrência

A atualização das duas memberships e de `project.managerId` ocorre numa única transação. As validações são concluídas antes das mutações, e qualquer falha provoca rollback completo.

Não foi necessária uma migration de base de dados, porque a operação reutiliza as colunas e os campos de versão já existentes.

### Testes adicionados

Foram adicionados 21 testes:

- 12 testes de serviço;
- 5 testes de controller;
- 4 testes HTTP de integração;
- atualização da cobertura da matriz de permissões.

### Validação direcionada

```text
Tests run: 26
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Validação completa

```text
Tests run: 236
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Impacto

- a transferência de ownership deixou de ser trabalho pendente;
- a matriz de permissões passou a incluir uma permissão exclusiva de transferência;
- o total da suite backend passou de 215 para 236 testes;
- a consistência entre `OWNER`, memberships e `project.managerId` ficou protegida por transação e optimistic locking.

### Próxima etapa

Abrir o pull request, solicitar review técnica ao Rúben e validar cuidadosamente autorização, concorrência, atomicidade e consistência de dados antes do merge.
