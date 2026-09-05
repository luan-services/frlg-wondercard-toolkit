# FRLG Wondercard Toolkit

MVP visual desktop em Java 21, JavaFX 21 e Maven. Artefato Maven: `frlg-wondercard-toolkit`.

## Executar

Instale JDK 21 e Maven, disponibilize ambos no PATH e execute na raiz:

```sh
mvn javafx:run
```

O primeiro uso exige internet para baixar dependências, incluindo JavaFX. Não é necessário instalar JavaFX separadamente.

```sh
mvn clean package
```

Esse comando compila e passa pela fase de testes. Ainda não há testes automatizados. O JAR gerado não é um instalador nem um executável com Java incluído.

## Estado atual

- Abas Injector e Builder vazias; Preset Composition contém o MVP.
- Seleção de FireRed/LeafGreen EN 1.0/1.1, arquivo WC3 e onze presets.
- Tema escuro sem sombras, primary rosa e paletas auxiliares no CSS.
- Lista com rolagem, chevron animado e barras de capacidade com cores por utilização.
- Generate abre uma prévia de sucesso com nomes fictícios; não gera arquivos.

Catálogo, validação, capacidades e hotkeys são mockados. Selecionar um arquivo não lê nem modifica seu conteúdo. Nenhuma dependência de ramscript-tools está integrada.

## Versionamento

O projeto está em `0.1.0-SNAPSHOT` (desenvolvimento). A primeira versão pode ser somente visual, desde que identificada como MVP mock. Commits registram mudanças; tags como `v0.1.0` marcam versões específicas; Releases podem acrescentar notas e instaladores. Ao publicar uma versão estável, alinhe a versão do POM e a tag. Não incluir `target/`, dependências baixadas ou arquivos pessoais de jogo no commit.

## Documentação

- [PROJECT_HANDOFF.md](PROJECT_HANDOFF.md): estado técnico e próximos passos.
- [Solicitações de integração](docs/RAMSCRIPT_INTEGRATION_REQUESTS.md): leitura do toolkit e contrato proposto para o outro projeto.
