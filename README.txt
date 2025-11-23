Lifesteal - PaperMC plugin (Java)
---------------------------------

Structure of the project (Maven):
lifesteal-papermc/
├─ pom.xml
├─ src/main/resources/plugin.yml
├─ src/main/resources/config.yml
└─ src/main/java/com/lifesteal/plugin/
   ├─ LifestealPlugin.java
   ├─ HeartListener.java
   └─ Commands.java

How to build:
1. Install Java 17+ and Maven.
2. In the project folder run: mvn clean package
3. The compiled jar will be in target/lifesteal-1.0.0.jar

How to install on server:
1. Copy lifesteal-1.0.0.jar to your Paper 1.21.4 server's plugins/ folder.
2. Start the server. Config will be auto-generated in plugins/Lifesteal/config.yml
3. Commands:
   - /withdrawheart (player, requires >10 hearts)
   - /createheart [amount] (op)
   - /revive <player> (op)

Notes:
- Default hearts: 10 (config: default-hearts)
- Max hearts: 20 (config: max-hearts)
- Min hearts: 1 (config: min-hearts)
- Item used for hearts: RED_DYE (config: heart-item.material)
