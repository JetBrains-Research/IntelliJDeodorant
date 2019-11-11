# IntelliJDeodorant
An IntelliJ IDEA plugin that detects code smells and recommends appropriate refactorings to resolve them. 

Based on [JDeodorant](https://github.com/tsantalis/JDeodorant) Eclipse plugin.

The original tool supports five code smells, namely **Feature Envy**, **Type/State Checking**, **Long Method**, **God Class** and **Duplicated Code**. 

*Work in progress. The check mark indicates that the code smell is supported by IntelliJDeodorant.*

- [x] Feature Envy problems can be resolved by appropriate Move Method refactorings.
- [ ] Type Checking problems can be resolved by appropriate Replace Conditional with Polymorphism refactorings.
- [ ] State Checking problems can be resolved by appropriate Replace Type code with State/Strategy refactorings.
- [x] Long Method problems can be resolved by appropriate Extract Method refactorings.
- [ ] God Class problems can be resolved by appropriate Extract Class refactorings.
- [ ] Duplicated Code problems can be resolved by appropriate Extract Clone refactorings.

## Installation

* Clone this repository
* Build .jar ```./gradlew jar``` 
* Go to ```Settings-> Plugins-> Install plugin from disk```
* Locate and select result .jar from the first step
* Restart IntelliJ IDEA

## Usage

The ```JDeodorant``` tool window will appear in IntelliJ IDEA. Each tab of the panel contains a ```Refresh``` button allows detecting corresponding code smell in the whole project and table with results. To apply any refactoring, you must check the corresponding checkbox in the leftmost column of the table and click the ```Refactor``` button.
