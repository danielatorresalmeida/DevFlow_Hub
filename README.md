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

## Tecnologias previstas

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
- H2 para testes isolados

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

Fundação do novo repositório criada.

O código da aplicação será adicionado progressivamente através de branches e commits organizados.

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
└── README.md
```

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