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
