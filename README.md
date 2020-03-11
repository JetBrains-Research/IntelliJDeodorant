<h1> <img align="left" width="50" height="50" src="https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/static/intellij-deodorant/icon.svg" alt="IntelliJDeodorant Icon"> IntelliJDeodorant </h1>

[![JB Research](https://jb.gg/badges/research-flat-square.svg)](https://research.jetbrains.org/)
[![CircleCI](https://img.shields.io/circleci/build/github/JetBrains-Research/IntelliJDeodorant.svg?style=flat-square)](https://circleci.com/gh/JetBrains-Research/IntelliJDeodorant)
[![Gitter](https://badges.gitter.im/intellijdeodorant/community.svg)](https://gitter.im/intellijdeodorant/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)

An IntelliJ IDEA plugin that detects code smells in Java code and recommends appropriate refactorings to resolve them. All of the suggested refactorings can be carried out automatically from within the plugin.

Based on [JDeodorant](https://github.com/tsantalis/JDeodorant) Eclipse plugin.

<p align="center">
  <img src="https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/static/intellij-deodorant/long-method.gif" width="90%"/>
</p>

## Supported code smells
The tool supports several code smells, namely Feature Envy, Type/State Checking, Long Method, and God Class.

- **Feature Envy** occurs when a method uses attributes/methods of another class more than those of the enclosing class. The tool can detect such methods and suggest moving them to a more related class, i.e. perform a **Move Method** refactoring.

- **Type Checking** relates to cases when an attribute, which determines the outcome of the program, is represented by complicated conditional statements. The tool detects such pieces of code and suggests a **Replace Conditional with Polymorphism** refactoring.

- **State Checking** relates to cases when a set of conditional statements determine the outcome of the program by comparing the value of a variable representing the current state of an object with a set of named constants. The tool detects sets like this and suggests a **Replace Type code with State/Strategy** refactoring.

- **Long Method**, as the name suggests, occurs when a method is too long and can be divided into several. For such methods, the tool identifies blocks of code that are responsible for calculating a variable and suggests extracting it into a separate method, i.e. perform an **Extract Method** refactoring.

- **God Class** is a name given to a large and complex class that contains too many components. The tool identifies sets of attributes and methods in a class that could be moved into a separate class to simplify the understanding of the code, i.e. an **Extract Class** refactoring can be performed.

## How to get
Supported IntelliJ IDEA version: 2019.3.3

1. Clone this repository.
2. Build IntelliJDeodorant.jar using ```./gradlew jar```.
3. Go to ```Settings -> Plugins -> Install plugin from disk```.
4. Select IntelliJDeodorant.jar.
5. Restart IntelliJ IDEA.

Done! The plugin is ready.

## Getting started
The ```IntelliJDeodorant``` tool window will appear in IntelliJ IDEA. Each tab of this window contains a ```Refresh``` button that allows to search for the necessary code smell in the entire project and the table with the results of the search. To apply any refactoring, simply select a suggestion in the table and click the ```Refactor``` button.

## Contacts
If you have any questions about the plugin or want to report any bugs, feel free to contact us using [Gitter](https://gitter.im/intellijdeodorant/community?utm_source=share-link&utm_medium=link&utm_campaign=share-link) or [GitHub Issues](https://github.com/JetBrains-Research/IntelliJDeodorant/issues).  
If you want to contribute, please create pull requests.
