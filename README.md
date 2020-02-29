# IntelliJDeodorant

[![JB Research](https://jb.gg/badges/research-flat-square.svg)](https://research.jetbrains.org/)
[![CircleCI](https://img.shields.io/circleci/build/github/JetBrains-Research/IntelliJDeodorant.svg?style=flat-square)](https://circleci.com/gh/JetBrains-Research/IntelliJDeodorant)

An IntelliJ IDEA plugin that detects code smells and recommends appropriate refactorings to resolve them. 

Based on [JDeodorant](https://github.com/tsantalis/JDeodorant) Eclipse plugin.

The tool supports several code smells, namely Feature Envy, Type/State Checking, Long Method, and God Class.

- **Feature Envy** problems can be resolved by appropriate Move Method refactorings.
  - The tool detects methods that use attributes/methods other class more than enclosing and suggests to move the method to the class to which it's more closely related.
- **Type Checking** problems can be resolved by appropriate Replace Conditional with Polymorphism refactorings.
  - The tool detects the pieces of code where a selection of program execution path depends on the value of an attribute, represented using complicated conditional statements.
- **State Checking** problems can be resolved by appropriate Replace Type code with State/Strategy refactorings.
  - The tool detects a set of conditional statements that select a program execution path by comparing the value of a variable representing the current state of an object with a set of named constants.
- **Long Method** problems can be resolved by appropriate Extract Method refactorings.
  - The tool  identifies blocks of code that responsible for calculation of variable and suggest to extract it into separate method.
- **God Class** problems can be resolved by appropriate Extract Class refactorings.
  - The tool identifies the set of attributes and method that could be moved into separate class to simplify understanding of the code. 

## Installation
Supported IntelliJ IDEA version: 2019.3.3

1. Clone this repository
2. Build IntelliJDeodorant.jar using ```./gradlew jar``` 
3. Go to ```Settings-> Plugins-> Install plugin from disk```
4. Locate and select IntelliJDeodorant.jar
5. Restart IntelliJ IDEA

## Usage

The ```IntelliJDeodorant``` tool window will appear in IntelliJ IDEA. Each tab of the panel contains a ```Refresh``` button allows detecting corresponding code smell in the whole project and table with results. To apply any refactoring, you must select suggestion in the table and click the ```Refactor``` button.

<p align="center">
  <img src="https://s3-eu-west-1.amazonaws.com/public-resources.ml-labs.aws.intellij.net/static/intellij-deodorant/long-method.gif" width="90%"/>
</p>

## Communication
Feel free report bugs and ask questions using [GitHub Issues](https://github.com/JetBrains-Research/IntelliJDeodorant/issues). If you want to contribute, please send us pull requests.

