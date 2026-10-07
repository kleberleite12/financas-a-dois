# Executar a suíte

Requer JDK 21. Não requer PostgreSQL, credenciais reais nem aplicação em execução.

Na raiz do checkout:

```sh
bash mvnw clean test
```

`bash mvnw` evita depender da permissão de execução do wrapper. Maven baixa suas dependências na primeira execução. Os relatórios ficam em `target/surefire-reports`.

Não é necessário sobrescrever a codificação de compilação ou dos recursos.

## Isolamento

`WebIntegrationTest` ativa o perfil `test` e sobrescreve os bindings de datasource e senhas com `@DynamicPropertySource`, com prioridade sobre variáveis da máquina. O datasource é H2 em memória, em modo de compatibilidade PostgreSQL. As senhas são fictícias e exclusivas de testes. A limpeza verifica o tipo de datasource antes de apagar os registros de teste.

Os dados de cada teste são criados e limpos nesse banco em memória. O teste `contextLoads` também herda essa configuração. Não habilite execução paralela desses testes de integração: eles compartilham um contexto e o mesmo banco, com limpeza por método.

`HomeControllerTest` usa Mockito e roda sem contexto Spring ou banco. Os outros testes exercitam requisições MockMvc, filtros reais do Spring Security, renderização Thymeleaf e repositório JPA real. As integrações compartilham um único contexto Spring em cache; cada caso é específico e independente dos dados dos anteriores.

MockMvc não imprime requisições, evitando registrar campos de senha. Não habilite dumps de requests, ambientes ou credenciais para diagnosticar falhas de autenticação.

## Organização

| Classe | Responsabilidade |
| --- | --- |
| `LancamentoSecurityTest` | Login real, logout, acesso anônimo, CSRF, autorização e IDs forjados |
| `LancamentoControllerTest` | Criação, edição, exclusão, validação HTTP e persistência |
| `HomeControllerTest` | Cálculos, metas, compatibilidade, precisão decimal e navegação |
| `HomeIntegrationTest` | Cenários financeiros solicitados através de HTTP e JPA |
| `LancamentoListingTest` | Visualização por pessoa, agrupamento, resumos e filtros |
| `DetalheControllerTest` | Cards, compatibilidade, total e parâmetros da rota |
| `TemplateRegressionTest` | HTML renderizado, moeda, percentuais, CSRF, escape e seletor de mês |
| `LancamentoRepositoryTest` | Persistência de enums e decimais, limites da consulta por datas |

Os testes do seletor confirmam que o HTML mantém `type="month"` e o `onclick` com `showPicker()`. A abertura nativa do seletor depende do navegador e não é simulada por MockMvc.

Veja [o relatório da revisão](REVISAO-TECNICA.md) para resultados, regras mapeadas, bugs corrigidos e limitações.
