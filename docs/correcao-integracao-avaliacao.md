# Integração com Avaliação Paciente

## Correção

O consumidor do Acolhimento foi alinhado ao contrato paginado do Avaliação Paciente. A lista pública do Acolhimento permanece um array dentro do envelope de sucesso.

| Fronteira | Contrato |
|---|---|
| Avaliação Paciente → Acolhimento | `GET /api/avaliacao/pacientes/paciente/{patientId}?page&size=20`, com `data={items,total,page,size}` |
| Detalhe canônico | `GET /api/avaliacao/pacientes/{avaliacaoId}?patientId={patientId}` |
| Acolhimento → Gateway → Dashboard | Endpoints e envelopes existentes, com `data=[]` para disponíveis e vínculos |

Todas as páginas são percorridas sequencialmente, sem filtrar modalidades, estados ou canceladas. O adapter confere paginação, total, identidade do paciente, duplicidade, correlação do header e do corpo e o código esperado. Título nulo continua nulo.

O prazo agregado é de 15 segundos, incluindo envio, páginas, leitura do corpo e validação. Cada resposta mantém o limite de 1 MiB. Não há retry automático. Uma falha intermediária descarta o resultado completo; não é convertida em lista parcial ou vazia.

O detalhe usa o par paciente–avaliação no carregamento e na criação de vínculos. Somente `404 AVALIACAO_NOT_FOUND` estruturado e correlacionado representa ausência; rota inexistente, resposta incompatível e falha de integração continuam produzindo `502 INTEGRATION_FAILURE`. Paciente estrangeiro recebe ausência canônica, sem consulta alternativa para descobrir seu proprietário.

Os logs técnicos registram operação, fase, código, correlação, duração, página e status remoto conhecido. Bearer, URL, corpo e conteúdo clínico não são incluídos.

## Verificação local

| Recorte | Estado |
|---|---|
| Gateway: contratos MVC/Feign afetados | PASS: 19 testes; MFA e Redis simulados nesse recorte |
| Dashboard: SDK, BFF e proxy afetados | PASS: 15 testes direcionados e suíte completa com 1.170 testes, lint dos arquivos afetados e tipos dos testes |
| Dashboard: build da aplicação | PASS |
| Portal: suporte compartilhado | PASS: lint, tipos dos testes e 621 testes |
| Rust: fmt, check, Clippy, suíte e build | PASS: 13 testes na suíte exata `cargo test --all-targets --locked`, com paralelismo padrão |
| PostgreSQL e RabbitMQ explícitos | PASS: dois casos executados separadamente pelo test executable compilado na suíte; mesmo banco descartável, sem reset entre os casos |
| Integração real | PASS: 78 etapas familiares, 67 requisições administrativas e 55 requisições do percurso do Acolhimento |
| Navegador | PASS: lista, seleção da avaliação na posição 24, vínculo confirmado e quatro vínculos preservados após recarga |
| Persistência | PASS: 54 avaliações disponíveis; snapshot, respostas, autoria e estado familiar `CONCLUIDA` preservados integralmente |
| Scanners | PASS/COMPLETE: zero erros e avisos; um informativo preexistente sobre `MessagePublisherPort.is_available`, que consulta somente o estado local do canal |
| Integridade das fontes | PASS: cinco snapshots Rust iguais aos do runtime e 2.244 arquivos de produção protegidos sem alterações |

Tentativas iniciais e falhas de preparação permanecem nos artefatos locais. Não são contadas como aprovação. Os testes de infraestrutura ignorados pela suíte padrão precisam ser executados explicitamente, primeiro PostgreSQL e depois RabbitMQ sobre o schema criado pelo primeiro.

O teste de deadline usa clock controlado, dois corpos atrasados oito segundos e a granularidade de um milissegundo do timer: permanece pendente em 14 segundos e falha no tick de 15 s + 1 ms. O orçamento de produção permanece em 15 segundos. O teste de diagnóstico captura os campos técnicos e verifica a ausência dos sentinelas de bearer, conteúdo clínico e URL.

O binário Windows validado tem SHA-256 `cfd6fd46fb8c8334d2fe80301f994e2b1f950694e92caafda637acab28007131`. O test executable dos dois casos de infraestrutura tem SHA-256 `27a3511431ef73e25c2ce7cbfd635644b83ea823c5c7df0423423259d914594c`. Não foi inferido build Linux ou resultado de CI remota a partir dessas execuções.

O suporte `humanizar-portal-familia/tests/integration/run_acolhimento_assessment.py` reutiliza as fixtures administrativas e familiares. Inicia serviços e dependências descartáveis com owner, banco próprio, vhost exclusivo e origens loopback. O script `humanizar-admin-dashboard/tests/integration/acolhimento-assessment-flow.ts` atravessa SDK, Route Handlers exportados, Gateway, Acolhimento e Avaliação Paciente reais.

As negativas anteriores ao encaminhamento têm contratos existentes distintos: sem cookies, o proxy do Next.js retorna `401` com `Não autorizado` e `Faça login primeiro`; com cookies e origem inválida, a rota BFF retorna `401` com `Nao autorizado` e `Sessao OIDC ausente ou invalida`. Não garantem o envelope remoto, ReasonCode ou correlação. Esses comportamentos são testados separadamente e não são apresentados como prova de autorização do backend. O código de produção do proxy e do BFF não foi alterado.

A tentativa integrada 3 confirmou os percursos positivos, mas falhou ao supor que a negativa do proxy era idêntica à da rota. Foi mantida como FAIL, com cleanup PASS. A tentativa 4 repetiu integralmente os três percursos com dados novos e encerrou as verificações HTTP como PASS. Os artefatos estão em `target/assessment-integration-b47b6f8e8d7d43b79e6f1797df71468a`; `final-validation-proof.json` registra a conferência posterior do banco e das fontes.

A verificação visual foi concluída na página de avaliações de anamnese. Os catálogos de núcleos, colaboradores e unidades permanecem fronteiras sintéticas do suporte; a visão geral apresentou aviso de nomes de núcleos e não constitui validação desses serviços. Nenhuma resposta de avaliações foi simulada na prova integrada.

Os recursos descartáveis são conferidos por identidade e proprietário antes da limpeza. O RabbitMQ disponibilizado pelo usuário continua separado dos testes que exigem o vhost fixo de contrato, executados em outro container exclusivo.

A limpeza final terminou como PASS para a fixture principal, o edge e o broker de contrato. Não restaram containers desta execução; o RabbitMQ e o Redis preexistentes do usuário foram preservados.

## Publicação

1. Revisar o diff e os gates finais do `humanizar-acolhimento`.
2. Publicar somente o Acolhimento corrigido, mantendo o Avaliação Paciente atual.
3. Conferir saúde e versões e testar, com conta sintética autorizada, disponíveis, vínculo novo e recarga de vínculos existentes.
4. Conferir ausência de `502 INTEGRATION_FAILURE` nesse percurso e a mesma correlação nos serviços.

Não há migração SQL, variável de ambiente ou configuração nova de Keycloak. O rollback desta correção isolada reintroduz a incompatibilidade com o produtor paginado; não deve ser apresentado como restauração funcional desse fluxo.

Gateway, Dashboard e Portal receberam somente testes ou suporte de validação por causa desta correção. Não precisam de deploy para entregar o ajuste funcional. Nenhum commit, push, deploy ou alteração de produção faz parte da execução local.
