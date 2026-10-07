# Revisão técnica e testes

## Resultado

Revisão da aplicação Java 21 / Spring Boot 4.1.1 realizada em 7 de outubro de 2026. Foram lidos os manifests e wrappers, configuração, segurança, todos os controllers, modelos, repositório, templates, CSS, documentação e teste existente antes de implementar a suíte.

Foram criados **146 novos casos executados**, contando cada combinação parametrizada como um caso. O `contextLoads` existente foi preservado e isolado. Resultado da execução final, confirmado nos XMLs do Surefire:

```text
Tests run: 147, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**147 passaram; 0 falharam; 0 erros; 0 ignorados.**

| Classe | Executados | Passaram | Falhas/erros/ignorados |
| --- | ---: | ---: | ---: |
| LancamentoControllerTest | 50 | 50 | 0 |
| LancamentoSecurityTest | 29 | 29 | 0 |
| HomeControllerTest | 16 | 16 | 0 |
| LancamentoListingTest | 15 | 15 | 0 |
| DetalheControllerTest | 13 | 13 | 0 |
| TemplateRegressionTest | 12 | 12 | 0 |
| LancamentoRepositoryTest | 8 | 8 | 0 |
| HomeIntegrationTest | 3 | 3 | 0 |
| FinancasADoisApplicationTests | 1 | 1 | 0 |
| **Total** | **147** | **147** | **0** |

A revisão preservou os usuários, o PostgreSQL de produção e as fórmulas financeiras. Não houve merge na main.

## Arquitetura e regras encontradas

A aplicação concentra regras nos controllers, usa JPA para `Lancamento` e renderiza HTML com Thymeleaf. Não há camada separada de serviços. `LancamentoRepository` fornece CRUD e consulta inclusiva por intervalo de datas. Os dois usuários são definidos por `InMemoryUserDetailsManager`; as senhas vêm das propriedades e são codificadas por BCrypt. O login é por formulário, com sessão e CSRF. Login e CSS são públicos; as demais rotas exigem autenticação.

- **Propriedade dos registros:** na criação, responsável é o nome do usuário autenticado. Edição e exclusão verificam o dono no banco, antes da escrita. Edição preserva ID e responsável; POST com ID inexistente retorna 404. Ler dados da outra pessoa não permite modificá-los.
- **Tipos ativos:** RENDA_PRINCIPAL, RENDA_EXTRA, CARTAO_CREDITO, OUTRO_GASTO e GUARDADO. RECEITA/GASTO continuam persistidos e lidos, mas não são aceitos em novos POSTs. O formulário de edição converte os tipos antigos para os equivalentes ativos sem persistir apenas pelo GET.
- **Valor:** obrigatório, positivo, no máximo 99.999.999,99. O servidor rejeita mais de duas casas significativas após `stripTrailingZeros`; `50.000` é aceito, enquanto `1.001` é rejeitado.
- **Período:** obrigatório e interpretado por `YearMonth.parse`; ao salvar, a data vira o primeiro dia do mês selecionado.
- **Campos opcionais:** descrição/categoria ausentes ou em branco recebem a descrição do tipo. Outros textos são aparados com `trim`; o limite de 255 é verificado antes desse trim.
- **Receitas:** RENDA_PRINCIPAL + RENDA_EXTRA + RECEITA antiga. **Gastos:** CARTAO_CREDITO + OUTRO_GASTO + GASTO antigo. **Disponível:** receitas − gastos − GUARDADO no mês, separadamente por pessoa.
- **Meta:** R$ 20.000 por pessoa. Acumulado soma GUARDADO de todos os meses, inclusive posteriores ao período exibido, conforme o código existente. Percentual usa divisão com escala 4 e HALF_UP, multiplicação por 100 e exibição com uma casa; barra visual é limitada a 100%, o percentual real não é.
- **Meses:** dashboard consulta primeiro ao último dia do mês, inclusive. Lista ordena data e ID decrescentes, agrupa por YearMonth e calcula resumos independentes.
- **Filtros:** combinam pessoa, período e receitas/gastos/guardado, incluindo tipos antigos equivalentes. Filtro inválido retorna à visualização normal. `quantidadeResultados` representa o total da pessoa, não a quantidade filtrada.
- **Detalhes:** filtram responsável, mês e tipo; cards de salário/cartão incluem tipos antigos. Total é a soma dos registros retornados.
- **Interface:** listagem padrão é da pessoa autenticada; `visualizacao=outro` inverte a pessoa exibida. Botões de alteração são ocultados na visualização do outro e a proteção também existe no backend. Valores monetários usam padrão brasileiro. O `showPicker()` foi preservado.

## Evidências de cobertura

| Verificação | Testes principais | Resultado |
| --- | --- | --- |
| Login obrigatório nas rotas principais, edição e POSTs | `anonymousCannotReadProtectedRoutes`, `anonymousCannotWriteEvenWithCsrf` | ✅ |
| Login real dos dois usuários, senha errada, usuário desconhecido e redirect | `realLoginRedirectsToDashboard`, `invalidLoginFails`, `successfulLoginOverridesPreviouslySavedRequest` | ✅ |
| Logout invalida sessão; sem sessão dashboard continua protegido | `logoutInvalidatesRealLoginSession` | ✅ |
| CSRF ausente/inválido e exclusão via GET bloqueados | `csrfIsRequiredForPost`, `invalidCsrfDoesNotCreateEntry`, `deletionCannotUseGet` | ✅ |
| Criar cada tipo para Kleber/Giovanna e ignorar responsável/data enviados | `createsEachActiveTypeAndIgnoresForgedOwnerAndDate` — 10 combinações | ✅ |
| Descrição/categoria opcionais, trim e limites | `blankOptionalFieldsUseTypeDescription`, `absentOptionalFieldsUseTypeDescription`, `trimsOptionalFields`, `acceptsValueBoundariesAndInsignificantTrailingZeros` | ✅ |
| Zero, negativo, excesso de valor/decimais, ausência de tipo/período, período inválido, tamanho de textos, tipos antigos enviados | `serverRejectsInvalidFieldsWithoutPersistence` — 17 combinações HTTP | ✅ |
| Tipos/textos não convertíveis não gravam dados | `invalidBindingIsBadRequestWithoutPersistence` — 4 combinações | ✅ |
| Editar próprio registro, todos os campos, sem mudar ID/dono nem duplicar; atualizar dashboard | `editsAllFieldsWithoutChangingIdOwnerOrCreatingDuplicate`, `invalidEditPreservesExistingRecord` | ✅ |
| Manipular ID do outro em GET/POST e excluir registro do outro, nos dois sentidos | `otherPersonsIdsCannotBeUsedToReadEditForm`, `forgedIdCannotModifyOtherPersonsEntry`, `forgedIdCannotDeleteOtherPersonsEntry` | ✅ |
| Visualizar outro não permite escrita | `viewingOtherPersonDoesNotGrantWritePermission`, `otherPersonsViewHasNoEditOrDeleteControls` | ✅ |
| Exclusão própria recalcula saldo/categoria/meta | `deletesOwnEntryAndRecalculatesSavingsGoalAndBalance`, `deletingExpenseReducesCategoryTotalAndIncreasesAvailableBalance` | ✅ |
| Kleber novembro: 3.500 − 1.250 − 600 = 1.650 | `kleberNovemberHasCorrectIncomeExpenseSavingsAndAvailable`, `requestedFinancialScenariosWorkThroughHttpAndJpa` | ✅ |
| Giovanna dezembro: 2.700 − 400 − 500 = 1.800 | `giovannaDecemberShowsHerOwnMainPanel`, `requestedFinancialScenariosWorkThroughHttpAndJpa` | ✅ |
| Guardado mensal 600/400; meta Kleber 1.000/5%; Giovanna 500/2,5% | `accumulatedGoalIncludesAllMonthsAndNeverMixesPeople`, `requestedFinancialScenariosWorkThroughHttpAndJpa` | ✅ |
| Compatibilidade RECEITA/GASTO em dashboard, filtros, detalhes e edição | `legacyTypesAreSalaryAndCreditCardInDashboard`, `legacyTypesRemainInQuickFiltersAndMonthlyTotals`, `legacyRecordsAppearInEquivalentCard`, `legacyEditFormMapsTypeWithoutWritingUntilSave` | ✅ |
| Meses separados, ordenação, resumos 3.500/1.250/600 | `monthGroupsAreDescendingWithIndependentSummariesAndNoLeakage`, `entriesAreSortedByDateThenDescendingId` | ✅ |
| Filtros receitas/gastos/guardado com total exato e pessoa correta | `quickFiltersReturnOnlySelectedMonthCategoryAndOwner`, `otherPersonsQuickFilterUsesOtherPersonsData`, `emptyFilteredMonthHasZeroTotal` | ✅ |
| Cinco cards respeitam período/dono/tipo e soma | `cardsRespectOwnerMonthAndTypeWithExactTotal` — 5 tipos | ✅ |
| Precisão decimal, saldo negativo, meta acima de 100%, meses vazios | `sumsDecimalAmountsExactly`, `negativeAvailableBalanceIsFlagged`, `progressBarCapsAt100WithoutCappingActualGoalPercentage`, `emptyMonthDoesNotLoseSavingsFromPriorMonths` | ✅ |
| Navegação anterior/próximo ano, ano bissexto e limites de mês | `monthNavigationCrossesYearBoundaries`, `monthNavigationLinksPreserveYearsAndMonths`, `leapYearIncludesLastDayButNotNeighbouringMonths`, `dateBetweenIncludesBothBoundariesAndExcludesOtherMonths` | ✅ |
| Moeda brasileira no HTML, percentuais, escape de conteúdo e picker | `dashboardRendersRequestedScenarioAndBrazilianCurrency`, `listingFormatsIndividualAmountsAsBrazilianCurrency`, `detailsTemplateFormatsRowsAndTotalAndLinksBackToSelectedMonth`, `goalPercentagesUseBrazilianDecimalComma`, `descriptionsAndCategoriesAreHtmlEscaped`, `newFormKeepsClickMonthPickerCsrfAndOnlyActiveTypes` | ✅ |

## Bugs encontrados e corrigidos

1. **Período malformado em `/detalhes` gera erro de servidor.** O parse não tinha tratamento de `DateTimeParseException`. `malformedPeriodReturnsBadRequestInsteadOfServerError` reproduziu três erros de execução com período inválido, vazio e texto arbitrário. Correção: tratar apenas essa exceção e retornar HTTP 400. Arquivo: `DetalheController.java`. Os três casos passaram após a correção; períodos válidos e seus cálculos continuam cobertos.

2. **Percentual da meta com separador decimal incorreto.** O template concatenava diretamente BigDecimal e `%`, exibindo `5.0%` e `2.5%`. `goalPercentagesUseBrazilianDecimalComma` falhou com essa saída. Correção: formatação Thymeleaf com vírgula e uma casa decimal apenas na apresentação. Arquivo: `templates/home.html`. O teste passou, sem alterar o valor da meta ou a fórmula.

3. **Build padrão falha ao ler `application.properties`.** O comentário de usuários tinha byte de codificação Latin-1, incompatível com UTF-8 usado pelo Maven Resources Plugin. Foi observado `MalformedInputException` na fase `resources`, antes de executar testes. Correção: converter somente esse comentário para UTF-8; chaves e valores de produção são idênticos. Arquivo: `application.properties`. A verificação que detecta e valida esse problema é a própria fase `resources` de `mvn clean test`, não um teste JUnit. A execução final passou sem sobrescrever encoding.

## Investigação e comandos

O primeiro comando `clean test`, usando a opção de codificação herdada do onboarding, encontrou uma colisão entre o helper `model(...)` e o matcher `model()` durante compilação dos testes. O helper foi renomeado para `viewModel(...)`: erro da implementação do teste, não da aplicação.

A primeira suíte compilada executou 144 casos: 135 passaram, 6 falharam e 3 tiveram erro. As três exceções de detalhes e a falha de percentual detectaram os dois bugs funcionais acima. Outras cinco falhas eram dos testes/configuração: três esperavam `R$ -valor`, quando NumberFormat produz `-R$ valor`; duas comparavam literais acentuados compilados com a codificação legada. Expectativas foram ajustadas e a codificação padrão restaurada após corrigir o comentário inválido. Nenhuma regra financeira foi adaptada para fazer um teste passar.

Com JDK 21, Maven cache local e settings de proxy do ambiente, foram executados os comandos abaixo. A listagem omite os argumentos locais de cache e proxy, que não são requisitos do projeto:

```sh
# Diagnóstico inicial: workaround herdado, removido depois da correção UTF-8.
bash mvnw \
  -Dproject.build.sourceEncoding=ISO-8859-1 -Dmaven.compiler.encoding=UTF-8 \
  -B -ntp clean test

# Primeira suíte compilada, após corrigir o helper.
bash mvnw \
  -Dproject.build.sourceEncoding=ISO-8859-1 -Dmaven.compiler.encoding=UTF-8 \
  -B -ntp test

# Após correções: 144/144 passaram.
bash mvnw -B -ntp clean test

# Execução final com isolamento reforçado e três casos HTTP adicionais: 147/147.
bash mvnw -B -ntp clean test

git diff --check
```

A última execução foi iniciada por Python `subprocess.run`, que preservou o código de saída do Maven. Recebeu variáveis de teste fictícias apontando para PostgreSQL inacessível e senhas sentinela, sem ler ou imprimir valores reais. Todos os casos continuaram usando o datasource H2 e as credenciais fictícias definidas no teste. Isso verifica a prioridade de `@DynamicPropertySource` sobre variáveis da máquina. O Maven terminou com status 0; os nove XMLs Surefire da execução final somaram 147 casos sem falhas/erros/ignorados.

## Arquivos criados

- `src/test/resources/application-test.properties`
- `src/test/java/io/github/kleberleite12/financas/support/WebIntegrationTest.java`
- `src/test/java/io/github/kleberleite12/financas/LancamentoSecurityTest.java`
- `src/test/java/io/github/kleberleite12/financas/LancamentoControllerTest.java`
- `src/test/java/io/github/kleberleite12/financas/HomeControllerTest.java`
- `src/test/java/io/github/kleberleite12/financas/HomeIntegrationTest.java`
- `src/test/java/io/github/kleberleite12/financas/LancamentoListingTest.java`
- `src/test/java/io/github/kleberleite12/financas/DetalheControllerTest.java`
- `src/test/java/io/github/kleberleite12/financas/TemplateRegressionTest.java`
- `src/test/java/io/github/kleberleite12/financas/LancamentoRepositoryTest.java`
- `docs/TESTES.md`
- `docs/REVISAO-TECNICA.md`

## Arquivos modificados

- `pom.xml`: H2, Spring Security Test e Jsoup, todos com scope `test`.
- `src/test/java/io/github/kleberleite12/financas/FinancasADoisApplicationTests.java`: teste existente herda isolamento H2.
- `src/main/java/io/github/kleberleite12/financas/controller/DetalheController.java`: HTTP 400 para período malformado.
- `src/main/resources/templates/home.html`: vírgula no percentual da meta.
- `src/main/resources/application.properties`: comentário em UTF-8; sem mudança nos valores de configuração.

## Limites e riscos remanescentes

- H2 em modo PostgreSQL valida o mapeamento e as consultas usadas, mas não substitui uma validação específica de PostgreSQL para migrações, diferenças de dialeto ou concorrência. Nesta suíte nenhum PostgreSQL foi conectado.
- MockMvc testa backend e HTML renderizado, não navegador real. A abertura visual nativa do seletor de mês e o layout responsivo não foram executados em browser.
- Datas fora dos intervalos usuais, especialmente anos extremos no dashboard/banco, não estão cobertas. `HomeController` chama `YearMonth.of` sem tratamento específico para anos fora do domínio; recomenda-se validar esses limites em uma tarefa posterior.
- Dashboard, listagem e detalhes carregam dados inteiros e filtram em memória; isso pode limitar desempenho com muitos registros. Consultas por responsável/tipo/período e paginação seriam melhorias futuras, sem necessidade de refatorar agora.
- O filtro de listagem captura `Exception` de forma ampla e pode ocultar erros inesperados. Foram preservados o fallback e o comportamento existentes.
- Não foram exercitadas concorrência de alterações, políticas de cookies HTTPS em produção ou rotinas de implantação. A revisão não equivale a uma auditoria de segurança externa nem a cobertura de todas as linhas.
- A suite compartilhada deve permanecer sequencial enquanto usar o mesmo banco H2/contexto.

As regras atuais, inclusive o acumulado de todos os meses, a compatibilidade dos tipos antigos, os usuários e a meta individual, foram preservadas. Consulte [TESTES.md](TESTES.md) para repetir a execução.
