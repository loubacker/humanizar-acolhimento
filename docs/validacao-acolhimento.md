# Validação do Acolhimento

Esta implementação está em andamento e não está pronta para publicação. Os resultados abaixo foram obtidos localmente em 28/10/2026; não comprovam o estado de produção.

| Cenário | Resultado | Evidência |
| --- | --- | --- |
| Anamnese por paciente e núcleo, autenticação e autorização | PASS | PostgreSQL isolado e rotas Axum com JWT e JWKS sintéticos |
| Escrita de seção de outro paciente e rollback | PASS | Teste HTTP com PostgreSQL isolado |
| Atualizações simultâneas e preservação de UUIDs | PASS | Duas requisições HTTP sincronizadas por bloqueio PostgreSQL |
| Criação de acolhimento, correlação e ator na Outbox | PASS | Teste HTTP e consulta ao PostgreSQL isolado |
| DLQ, indisponibilidade da quarentena e repetição divergente | PASS | RabbitMQ e PostgreSQL isolados; mensagem retida até persistência e evento divergente em quarentena |
| Evento de equipe e consulta cadastral do Client Família | PASS parcial | Testes de serviço da Notification; não foi executado o conjunto completo de serviços reais |
| Comando de paciente no consumidor do Client Família | PASS parcial | Sete testes com PostgreSQL e RabbitMQ isolados, incluindo binding real e callback durável; o produtor Notification foi representado pelo teste |
| Comando de consulta no consumidor do Núcleo | PASS parcial | PostgreSQL e RabbitMQ isolados, com confirmação de publicação, callback e redelivery; o produtor Notification foi representado pelo teste |
| Acolhimento → Notification → Núcleo e Client | PASS parcial | Processos reais com Keycloak, PostgreSQL e RabbitMQ descartáveis: criação HTTP `201`, Outbox publicada e respostas de Núcleo e contato persistidas; replay manteve um único evento e vínculo |
| Callback cadastral e envio de e-mail | PASS parcial | Consumidor e persistência reais do Client Família; fixture publicou o callback persistido; Notification registrou sucesso e o SMTP local capturou um e-mail sintético. O relay do Client Família e o envio de WhatsApp não foram exercitados nesse cenário |
| MFA do Client → Gateway → Acolhimento | PASS parcial | Keycloak, Client e Gateway reais em ambiente descartável: MFA verificado, atualização HTTP `200` e consulta da intenção com estado `COMPLETED` |
| DLQ da Notification durante falha e recuperação do banco | PASS | Teste com RabbitMQ e PostgreSQL isolados |
| Erro, toast e retentativa da lista de avaliações | PASS | Teste DOM isolado; não substitui validação visual em navegador |
| Jornada integrada Dashboard → Gateway → Acolhimento → Notification → Client Família | NOT_EXECUTED | Falta executar com todos os serviços e Keycloak isolados |
| Identidade durável e consulta por ator | PASS parcial | As oito ações passaram em concorrência com a mesma chave, replay de sucesso e rejeição, conflito de conteúdo ou recurso, acesso por outro ator e replay sem autenticação; ausência do resultado armazenado após o commit foi simulada para as oito ações |
| Avaliação pertence ao paciente, título e situação canônicos | PASS parcial | HTTP do Acolhimento e PostgreSQL reais; Avaliação Paciente foi representada por fixture HTTP compatível com a fonte inspecionada |
| Avaliação inexistente e rota da dependência inexistente | PASS parcial | O teste HTTP distingue `AVALIACAO_NOT_FOUND` de `ROUTE_NOT_FOUND` na fixture da dependência, inclusive na consulta da lista; falta o teste com o serviço Avaliação Paciente real |
| Formulário bloqueado após falha de carregamento e resposta antiga | PASS | Testes DOM dos componentes com transporte simulado |
| Reconciliação das solicitações antigas ao Patient Service | INCOMPLETE | Há um inventário de leitura e procedimento preparado na Notification; não existe executor automático testado |

Os testes Rust externos que exigem infraestrutura foram executados separadamente após o teste padrão. A execução padrão marcou os dois testes de integração como ignorados; as execuções explícitas de PostgreSQL/HTTP e RabbitMQ passaram. `cargo fmt --check`, `cargo check --all-targets --locked`, Clippy com `-D warnings` e os scanners Rust de estilo e arquitetura passaram.

A Notification executou 273 testes, com zero falhas e nove ignorados por infraestrutura. Cinco desses testes foram executados separadamente com PostgreSQL e RabbitMQ isolados, com sucesso; quatro testes de outros fluxos permaneceram ignorados. O Gateway passou em 613 testes, sem falhas ou testes ignorados, em uma cópia sem artefatos de build. Os arquivos do recorte de Acolhimento e suas configurações estavam iguais aos do workspace após a suíte; três arquivos de outras áreas mudaram enquanto ela rodava. O recorte de transporte também confirmou que a escrita não foi repetida e o redirecionamento não foi seguido.

O Dashboard passou em 1.077 testes, sem falhas, e os hashes dos arquivos do recorte de Acolhimento permaneceram iguais durante a execução. Três arquivos gerais foram ajustados por tipo e formatação durante a suíte; os três testes da preferência da barra lateral passaram separadamente após o ajuste. O `check`, a geração de tipos de rotas, os tipos da aplicação e dos testes e o build final passaram.

No primeiro cenário integrado, as consultas de paciente e contato permaneceram pendentes porque Client Família e Client não estavam em execução. O broker retornou esses comandos sem rota e a Outbox da Notification os registrou como falhas de publicação com retentativa. Em um segundo cenário, com o Client ativo, a resposta de contato foi persistida; a consulta de paciente permaneceu pendente. Não houve envio externo nem validação de entrega nesses cenários.

No terceiro cenário, a composição de teste iniciou o consumidor real de consulta cadastral e persistiu o recibo e a resposta na Outbox do Client Família. A própria fixture acionou o publisher para essa resposta; ela não iniciou o relay nem o processo completo do Client Família. A Notification registrou as três dependências como concluídas e o envio de e-mail como bem-sucedido. O Mailpit capturou uma única mensagem destinada a `example.invalid`, com os dados sintéticos esperados; a repetição da intenção HTTP não gerou outra mensagem. Essa captura local não comprova entrega em caixa postal externa.

Em um teste separado, uma identidade sintética obteve token no Keycloak descartável e concluiu o MFA real do Client. A atualização passou pelo Gateway real até o Acolhimento, preservando a correlação e a chave da operação; a consulta pelo Gateway e pelo banco confirmou `COMPLETED`. A inicialização do Dashboard de teste foi bloqueada pela política automática da ferramenta. Portanto, esse teste não comprova o BFF nem a interface no navegador.

Esses testes entre processos ocorreram antes do último ajuste no tratamento de `404` da dependência de avaliação. Depois do ajuste, foram repetidos os gates Rust e os testes HTTP com PostgreSQL e RabbitMQ isolados; a composição entre processos não foi repetida com essa última versão.

## Pendências antes da publicação

1. Ampliar os testes de queda de processo ou de rede após o commit, reinício e concorrência entre intenções diferentes para as oito ações de forma individual. A ausência do resultado armazenado foi simulada por alteração na fixture PostgreSQL, sem simular queda real de processo.
2. Avaliar a antiga rota BFF de questionários, ainda registrada e não validada com o Rust, sem assumir equivalência entre identificadores diferentes. A página de detalhe antiga oferece um link para a área canônica de avaliações.
3. Implementar e testar a reconciliação das dependências históricas conforme o procedimento `docs/acolhimento-dependencias-patient.md` da Notification.
4. Executar o fluxo completo com Dashboard, Gateway, Acolhimento, Notification, Client Família, Client, Núcleo, Staff, Avaliação, Avaliação Paciente, PostgreSQL, RabbitMQ, Redis e Keycloak isolados, além da validação visual em desktop e celular.
5. Repetir as verificações gerais sobre um snapshot integral estabilizado antes da publicação.
6. Antes de qualquer publicação, conferir o schema de cada banco. O pacote do Acolhimento inclui as migrações `0002_dead_letter_quarantine.sql`, `0003_acolhimento_operations.sql` e `0004_avaliacao_titulo_nullable.sql`; a Notification depende da quarentena prevista na migração de avaliação. O estado dos bancos existentes não foi conferido nesta tarefa, e nenhum script foi aplicado em produção.
7. Configurar o endereço interno `AVALIACAO_PACIENTE_URL` no Acolhimento; não foi criada configuração de cliente de Avaliação no Keycloak.
8. Conferir e, quando autorizado, ativar `PATIENT_NOTIFICATION_RETRIEVE_ENABLED=true` no Client Família. O consumidor da consulta cadastral enviada pela Notification é desativado por padrão; sem ele, as dependências de paciente não avançam.

Para a integração integral, a fixture existente de cadastro usa Keycloak HTTP, mas a configuração de produção do Client Família exige HTTPS para o emissor e o JWKS. Será necessário fornecer um Keycloak descartável com TLS confiável pelos processos isolados, por exemplo com uma CA de teste montada somente nos contêineres, sem alterar o armazenamento de certificados do host. Os binários ou imagens dos serviços devem corresponder aos hashes de suas fontes; a fixture de cadastro atual usa uma composição de teste e não inicia o consumidor de consulta de paciente.

O Patient Service Java permanece fora deste trabalho. Commit, push, alteração de banco ou Keycloak de produção e deploy exigem autorização específica.
