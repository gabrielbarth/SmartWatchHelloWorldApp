# Documentação de Fluxo: Projeto de Comunicação Celular-Relógio

Este documento detalha o processo de desenvolvimento, os desafios encontrados e as soluções aplicadas na criação de um projeto de comunicação bidirecional entre um aplicativo de celular e um de Wear OS.

---

## Fase 1: Estrutura Inicial e Comunicação Básica

O objetivo inicial era estabelecer uma comunicação simples entre um celular e um relógio.

1.  **Criação dos Módulos**: O projeto foi iniciado com dois módulos principais no Android Studio:
    *   `:mobile`: Para o aplicativo do celular.
    *   `:wear`: Para o aplicativo do Wear OS.

2.  **Implementação da Comunicação**: A `MessageClient` da API Wearable foi escolhida como a tecnologia de comunicação. A lógica foi implementada diretamente nas `Activity`s de cada módulo.
    *   Um "caminho" (`/msg`) foi definido como o canal de comunicação para as mensagens.
    *   As operações de envio de mensagem, que são assíncronas, foram envolvidas em `coroutines` (`viewModelScope.launch` e `suspend fun`).

3.  **Primeiro Teste**: O aplicativo `mobile` enviava uma mensagem de texto fixa, e o `wear` fazia o mesmo, exibindo a última mensagem recebida.

---

## Fase 2: Minha Jornada de Depuração da Conexão

Nesta fase, eu esbarrei no maior desafio do projeto: a comunicação simplesmente não funcionava. O erro `API: Wearable.API is not available` aparecia constantemente, e a investigação para resolvê-lo me levou a uma série de descobertas sobre a configuração do ambiente.

1.  **Primeiro Obstáculo (no Relógio)**: O primeiro erro surgiu no relógio. A causa era simples: eu estava tentando rodar o app `wear` em um emulador de celular comum. A solução foi rápida: garantir que o alvo de execução fosse sempre um emulador de Wear OS.

2.  **O Problema Muda de Lado**: Mesmo com a correção, o erro passou a aparecer no celular. Foi aí que percebi que a causa era a configuração do meu ambiente de testes: eu estava usando meu **celular físico** para tentar me comunicar com um **emulador de relógio**.

3.  **Descobrindo a Solução de Rede (`adb forward`)**: Entendi que um dispositivo físico e um emulador não se encontram na mesma rede. Para resolver isso, precisei criar uma "ponte" de rede usando o Android Debug Bridge (ADB) no terminal:
    ```sh
    adb forward tcp:5601 tcp:5601
    ```

4.  **As Peculiaridades do ADB**: Usar o ADB, no entanto, não foi tão direto:
    *   **`no devices found`**: De início, o ADB não encontrava meu celular. A solução foi ativar a "Depuração USB" e as "Opções do Desenvolvedor" no aparelho.
    *   **Conexão via Wi-Fi**: Notei que meu celular estava conectado via "Wireless Debugging", o que fazia o comando `adb -d` (para USB) falhar. precisei então usar um comando mais específico para direcionar a ponte de rede: `adb -s <device_id> ...`.

5.  **A Pista Final (`Nós encontrados: 0`)**: Com a rede funcionando, a comunicação ainda falhava. Para descobrir o porquê, adicionei logs ao código que me deram a pista definitiva: a mensagem `Nós (relógios) encontrados: 0`. Isso provou que o problema não era mais de rede, mas de **pareamento**.

6.  **A Solução Definitiva**: A solução final foi abrir o aplicativo "Wear OS" no meu celular, desconectar qualquer pareamento antigo e **refazer um novo pareamento** com o emulador. Após esse passo e reativar a ponte ADB, a comunicação finalmente funcionou como esperado.

---

## Fase 3: Refatoração e Melhoria da Interface

Com a comunicação estável, o foco mudou para a qualidade do código e a experiência do usuário.

1.  **Introdução do `ViewModel` (no `:mobile`)**: Para seguir as práticas modernas do Android, a lógica de comunicação do módulo `mobile` foi movida da `MainActivity` para uma nova classe, `MainViewModel`.
    *   Isso separou a UI da lógica de dados.
    *   Usamos um `StateFlow` no `ViewModel` para expor a mensagem recebida. A UI observa esse fluxo e se atualiza automaticamente, de forma reativa.

2.  **Campo de Texto Dinâmico**: Um `OutlinedTextField` foi adicionado à UI do celular, permitindo que o usuário digite e envie mensagens personalizadas, em vez de um texto fixo.

---

## Fase 4: Personalização Visual e Funcional

1.  **Adição do Logo**: O logo da UTFPR foi adicionado aos dois aplicativos.
    *   A imagem foi colocada na pasta `res/drawable` de cada módulo.
    *   O componente `Image` do Jetpack Compose foi usado para exibir o logo na tela, carregando-o com `painterResource(id = R.drawable.logo)`.

2.  **Mensagem Dinâmica do Relógio**: Para tornar a comunicação mais interativa, a mensagem enviada pelo relógio foi aprimorada para incluir a hora atual. Foi usada a classe `LocalTime` do Java para capturar e formatar a hora no momento do clique.

## Estado Final do Projeto

Ao final deste fluxo, o projeto alcançou um estado robusto e funcional:

*   **Comunicação Bidirecional**: O celular envia texto digitado pelo usuário; o relógio responde com uma saudação e a hora atual.
*   **Arquitetura Limpa (no Celular)**: O uso de `ViewModel` e `StateFlow` garante um código organizado e resiliente a mudanças de configuração (como rotação de tela).
*   **Interface Personalizada**: Ambos os aplicativos exibem o logo da UTFPR, criando uma identidade visual consistente.
*   **Processo de Debug Documentado**: Os desafios de conexão, principalmente ao usar um dispositivo físico com um emulador, foram resolvidos e documentados.
