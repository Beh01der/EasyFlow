EasyFlow
========
by [DataSymphony.com.au](http://datasymphony.com.au/)

EasyFlow is a simple and lightweight Finite State Machine for Java

With **EasyFlow** you can:
* implement complex logic but keep your code simple and clean
* handle asynchronous calls with ease and elegance
* avoid concurrency by using event-driven programming approach
* avoid *StackOverflow* error by avoiding recursion
* simplify design, programming and testing of complex java applications

All this in less then 30kB and no run-time overhead!
[Download EasyFlow 1.1](http://datasymphony.com.au/?wpdmact=process&did=MS5ob3RsaW5r)

Here is a simple example illustrating how a state machine can be definded and implemented with **EasyFlow**

This is a State diargam fragment describing a simple ATM workflow

![State diagram fragment](http://datasymphony.com.au/wp-content/uploads/2013/04/atm_example.png)

With **EasyFlow** we can define the above state machine like this

```java
EasyFlow<FlowContext> flow = FlowBuilder

    .from(SHOWING_WELCOME).transit(
        onCardPresent.to(WAITING_FOR_PIN).transit(
            onPinProvided.to(...).transit(
                ...
            ),
            onCancel.to(RETURNING_CARD).transit(
                onCardExtracted.to(SHOWING_WELCOME)
            )
        )
    )
```
then all what's left to do is to implement our state handlers like so
```java
SHOWING_WELCOME.whenEnter(new StateHandler<FlowContext>() {
    @Override
    public void call(State<FlowContext> state, final FlowContext context) throws Exception {
        ...
        btnOption1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCardPresent.trigger(context);
            }
        });
        ...
    }
});
...
```
and start the flow
```java
flow.start(new FlowContext());
```
See the [complete example](https://github.com/Beh01der/EasyFlow-example-AtmEmulator/blob/master/src/au/com/ds/ef/ae/AtmEmulator/MainActivity.java)

To start using EasyFlow on your project, define Maven dependency like so
```xml
<dependency>
    <groupId>au.com.datasymphony</groupId>
    <artifactId>EasyFlow</artifactId>
    <version>1.2</version>
</dependency>
```

License [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)
