# ReadyToMove

ReadyToMove é uma aplicação nativa Android desenvolvida em Kotlin e Jetpack Compose, focada na gamificação da mobilidade urbana sustentável. A aplicação monitoriza as deslocações dos utilizadores em transportes suaves (como bicicletas e trotinetas) e transportes públicos, convertendo a distância percorrida em pontos num sistema de ranking competitivo.

## Visão Geral do Projeto

Este projeto foi desenvolvido no âmbito da unidade curricular de Computação Móvel. A solução implementa uma arquitetura robusta baseada em MVVM (Model-View-ViewModel), garantindo a persistência de dados em cenários offline e a sincronização com a cloud.

### Funcionalidades Principais

* **Navegação e Mapas:** Visualização da localização do utilizador e pontos de interesse (paragens de autocarro, bicicletas) utilizando o Google Maps SDK.
* **Monitorização de Viagem (Tracking):** Sistema de rastreamento GPS contínuo, implementado através de Foreground Services, garantindo a recolha de dados mesmo com a aplicação em segundo plano ou o dispositivo bloqueado.
* **Gestão de Perfil:** Autenticação de utilizadores e personalização de perfil, incluindo captura de fotografia via câmara.
* **Histórico e Leaderboard:** Registo detalhado de viagens e sistema de classificação global de utilizadores baseado na distância percorrida.
* **Interação com Hardware:**
    * Leitura de códigos QR para início de viagem (ML Kit).
    * Captura de imagem via CameraX.
    * Adaptação de tema (Claro/Escuro) baseada no sensor de luminosidade do dispositivo.

## Stack Tecnológica

O projeto utiliza as seguintes tecnologias e bibliotecas:

* **Linguagem:** Kotlin
* **Interface (UI):** Jetpack Compose (Material Design 3)
* **Arquitetura:** MVVM (Model-View-ViewModel)
* **Navegação:** Jetpack Navigation Compose
* **Assincronismo:** Kotlin Coroutines & Flow
* **Base de Dados Local:** Room Database
* **Backend & Autenticação:** Firebase Firestore & Firebase Authentication
* **Serviços de Localização:** Google Fused Location Provider
* **Mapas:** Google Maps SDK for Android
* **Câmara e Leitura de Códigos:** CameraX API & ML Kit (Barcode Scanning)
* **Networking:** Retrofit & OkHttp
* **Carregamento de Imagens:** Coil

## Configuração e Instalação

### Pré-requisitos
* Android Studio (Versão recomendada: Koala ou superior).
* JDK 17 ou superior configurado no projeto.
* Dispositivo físico ou emulador com Android 8.0 (API 26) ou superior.

### Configuração do Ambiente

1.  **Clonar o Repositório:**
    Clone o projeto para a sua máquina local.

2.  **Configuração de Chaves de API:**
    Para o correto funcionamento dos mapas e serviços de geocodificação, certifique-se de que as chaves de API (Google Maps e GeoApify) estão configuradas no ficheiro `local.properties` ou `AndroidManifest.xml`, conforme a implementação de segurança adotada.

3.  **Configuração do Emulador (Para leitura de QR Code):**
    Caso utilize o emulador Android para testar a funcionalidade de leitura de QR Code:
    * Abra o **Device Manager** no Android Studio.
    * Edite o dispositivo virtual (AVD) e aceda a **Show Advanced Settings**.
    * Na secção **Camera**, altere a configuração da câmara traseira ("Back") para **Webcam0**.
    * Reinicie o emulador.

## Guia de Validação e Testes

Para facilitar a validação das funcionalidades sem necessidade de registo, utilize as seguintes credenciais de teste pré-configuradas:

| Utilizador | Email | Password | Notas |
| :--- | :--- | :--- | :--- |
| **Utilizador Principal** | paraty@gmail.com | 123123 | Contém histórico de viagens. |
| **Utilizador Secundário** | leandro@gmail.com | 123123 | Utilizado para validar o ranking. |

### Passos para Validação das Funcionalidades

1.  **Tracking em Background:** Inicie uma viagem e minimize a aplicação. Verifique a notificação persistente na barra de estado que indica a execução do Foreground Service.
2.  **QR Code:** No ecrã do mapa, utilize a opção de iniciar viagem e aponte a câmara para um código QR contendo o texto "TYPE:BUS" ou "TYPE:BIKE".
3.  **Foto de Perfil:** Aceda ao perfil e clique no avatar para testar a integração com a CameraX.
4.  **Sensor de Luz:** Em ambientes escuros (ou simulando no emulador), a aplicação deve sugerir ou alterar para o tema escuro automaticamente.

## Estrutura do Projeto

A estrutura de pacotes segue uma organização lógica por camadas e funcionalidades:

* **data:** Contém os modelos de dados, repositórios e fontes de dados (local e remota).
* **ui:** Contém todos os ecrãs (screens), componentes reutilizáveis e definições de tema.
* **viewmodel:** Contém a lógica de negócio e gestão de estado da UI.
* **service:** Contém a implementação do LocationService (Foreground Service).
* **util:** Contém classes utilitárias e extensões.

## Autores

Desenvolvido no âmbito da disciplina de Computação Móvel.