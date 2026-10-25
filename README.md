# Zelcar para Android (Kotlin + Jetpack Compose)

Implementação do protótipo "Zelcar · Android" (Material 3, tema claro/escuro, 17 telas) com o design system "Industry".

## Como abrir
1. Android Studio Ladybug ou mais novo, JDK 17.
2. `File > Open` nesta pasta e aguarde o sync do Gradle (minSdk 26, compileSdk 35).
3. Rode o módulo `app`. Na primeira abertura use **Explorar com dados de exemplo** para ver o app como no protótipo.

## Estrutura (`app/src/main/java/com/zelcar/app`)
- `data/` Room (carros, gastos, lembretes, notas, checklist), DataStore (plano, preferências), dados de exemplo.
- `domain/` formatação pt-BR, ranking de lembretes, custo/km, checklist de 23 itens.
- `ui/theme` tokens do design system (claro/escuro) e fontes Barlow / Barlow Condensed.
- `ui/components` blueprint, tags, botões, campos, ícones de traço, scanner de QR.
- `ui/screens` onboarding, garagem, ficha, checklist, gastos, lembretes, diário, ajustes, pareamento, assinatura.
- `ui/sheets` ModalBottomSheets: gasto, nota, problema, km, carro, transferência, relatório, seletor do checklist.
- `work/` WorkManager: aviso diário agrupado de lembretes e resumo mensal. `report/` PDF com PdfDocument. `qr/` QR com ZXing.

## O que ainda é provisório
- **Assinatura**: Play Billing está ligado; crie no Play Console as assinaturas `zelcar_pro_monthly` e `zelcar_pro_annual`. Em build debug sem Play Store, "Assinar" ativa o Pro localmente.
- **Sincronização e transferência/pareamento**: o protótipo não define backend. `SyncService` e `TransferService` (em `data/Services.kt`) são implementações locais; troque por chamadas à API. O QR de transferência carrega um token local.
- **FIPE**: apenas o aviso de Pro; não há consulta real.

## Decisões fora do protótipo
Estados vazios, dados de exemplo opcionais, nome editável e exclusão de lembretes, seletor de fotos real, e o ícone de menu das abas raiz volta para a Garagem.

> Este código foi escrito sem acesso ao SDK do Android, então ainda não foi compilado. Se o primeiro build apontar algum erro, é ajuste pontual.
