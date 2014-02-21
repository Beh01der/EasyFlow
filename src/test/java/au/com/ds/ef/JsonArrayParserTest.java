package au.com.ds.ef;

import au.com.ds.ef.call.ContextHandler;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static au.com.ds.ef.FlowBuilder.from;
import static au.com.ds.ef.FlowBuilder.on;
import static au.com.ds.ef.JsonArrayParserTest.Events.*;
import static au.com.ds.ef.JsonArrayParserTest.States.*;
import static org.junit.Assert.*;

/**
 * User: andrey
 * Date: 11/04/13
 * Time: 12:05 PM
 */
public class JsonArrayParserTest {
    protected static class Item {
        enum ItemType {VALUE, LIST}

        private ItemType type;
        private List<Item> list;
        private String value;

        public Item(List<Character> value) {
            this.type = ItemType.VALUE;
            StringBuilder sb = new StringBuilder();
            for (Character c : value) {
                sb.append(c);
            }

            this.value = sb.toString();
        }

        public Item() {
            this.type = ItemType.LIST;
            this.list = new ArrayList<Item>();
        }

        public List<Item> getList() {
            return list;
        }

        public String getValue() {
            return value;
        }

        public ItemType getType() {
            return type;
        }
    }

    protected static class ParserContext extends StatefulContext {
        private String json;
        private int position;
        private char thisChar;
        private List<Character> thisValue;
        private boolean startedValue;
        private LinkedList<Item> stack;
        private Item result;

        public ParserContext(String json) {
            this.json = json;
            this.position = -1;
            this.thisChar = 0;
            this.stack = new LinkedList<Item>();
            this.thisValue = new ArrayList<Character>();
            this.startedValue = false;
        }

        public char getNextChar() {
            thisChar = ++position < json.length() ? json.charAt(position) : 0;
            return thisChar;
        }

        public char getThisChar() {
            return thisChar;
        }

        public void pushStack() {
            Item item = new Item();
            if (stack.isEmpty()) {
                result = item;
            } else {
                stack.getLast().getList().add(item);
            }

            stack.add(item);
        }

        public void popStack() {
            stack.removeLast();
        }

        public int getStackLevel() {
            return stack.size();
        }

        public boolean isStartedValue() {
            return startedValue;
        }

        public void startValue() {
            startedValue = true;
            thisValue.clear();
        }

        public void appendToValue() {
            thisValue.add(thisChar);
        }

        public void endValue() {
            startedValue = false;
            stack.getLast().getList().add(new Item(thisValue));
        }

        public Item getResult() {
            return result;
        }

        public void resetResult() {
            result = null;
        }
    }

    public enum States implements StateEnum {
        GETTING_CHAR,
        DONE,
        ERROR,
        VALIDATING,
        PROCESSING_CHAR,
        STARTING_ARRAY,
        ENDING_ARRAY,
        STARTING_VALUE,
        CONTINUING_VALUE,
        ENDING_VALUE,
        PROCESSING_SPACE
    }

    public enum Events implements EventEnum {
        noMoreChars,
        newChar,
        arrayStart,
        arrayEnd,
        valueStart,
        valueContinue,
        valueEnd,
        space,
        unexpectedChar,
        charProcessed,
        contentValid,
        contentInvalid
    }

    private EasyFlow<ParserContext> flow;

    @Before
    public void setUp() {
        flow =
            from(GETTING_CHAR).transit(
                on(newChar).to(PROCESSING_CHAR).transit(
                    on(arrayStart).to(STARTING_ARRAY).transit(
                        on(charProcessed).to(GETTING_CHAR)
                    ),
                    on(arrayEnd).to(ENDING_ARRAY).transit(
                        on(charProcessed).to(GETTING_CHAR),
                        on(unexpectedChar).finish(ERROR)
                    ),
                    on(valueStart).to(STARTING_VALUE).transit(
                        on(charProcessed).to(GETTING_CHAR)
                    ),
                    on(valueContinue).to(CONTINUING_VALUE).transit(
                        on(charProcessed).to(GETTING_CHAR)
                    ),
                    on(valueEnd).to(ENDING_VALUE).transit(
                        on(charProcessed).to(GETTING_CHAR)
                    ),
                    on(space).to(PROCESSING_SPACE).transit(
                        on(charProcessed).to(GETTING_CHAR)
                    ),
                    on(unexpectedChar).finish(ERROR)
                ),
                on(noMoreChars).to(VALIDATING).transit(
                    on(contentValid).finish(DONE),
                    on(contentInvalid).finish(ERROR)
                )
            );

        flow
            .executor(new SyncExecutor())

            .whenEnter(GETTING_CHAR, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    if (context.getNextChar() == 0) {
                        context.trigger(noMoreChars);
                    } else {
                        context.trigger(newChar);
                    }
                }
            })

            .whenEnter(PROCESSING_CHAR, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    char c = context.getThisChar();
                    if (context.isStartedValue() && c != '\'') {
                        context.trigger(valueContinue);
                        return;
                    }

                    switch (c) {
                        case '[':
                            context.trigger(arrayStart);
                            break;
                        case ']':
                            context.trigger(arrayEnd);
                            break;
                        case '\'':
                            if (context.isStartedValue()) {
                                context.trigger(valueEnd);
                            } else {
                                context.trigger(valueStart);
                            }
                            break;
                        case ' ':
                        case ',':
                            context.trigger(space);
                            break;
                        default:
                            context.trigger(unexpectedChar);
                    }
                }
            })

            .whenEnter(STARTING_ARRAY, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    context.pushStack();
                    context.trigger(charProcessed);
                }
            })

            .whenEnter(ENDING_ARRAY, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    if (context.getStackLevel() > 0) {
                        context.popStack();
                        context.trigger(charProcessed);
                    } else {
                        context.trigger(unexpectedChar);
                    }
                }
            })

            .whenEnter(STARTING_VALUE, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    context.startValue();
                    context.trigger(charProcessed);
                }
            })

            .whenEnter(CONTINUING_VALUE, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    context.appendToValue();
                    context.trigger(charProcessed);
                }
            })

            .whenEnter(ENDING_VALUE, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    context.endValue();
                    context.trigger(charProcessed);
                }
            })

            .whenEnter(PROCESSING_SPACE, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    context.trigger(charProcessed);
                }
            })

            .whenEnter(VALIDATING, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    if (context.getStackLevel() != 0) {
                        context.trigger(contentInvalid);
                    } else if (context.isStartedValue()) {
                        context.trigger(contentInvalid);
                    } else {
                        context.trigger(contentValid);
                    }
                }
            })

            .whenEnter(ERROR, new ContextHandler<ParserContext>() {
                @Override
                public void call(ParserContext context) throws Exception {
                    context.resetResult();
                }
            });
    }

    @Test
    public void testEmpty() {
        ParserContext ctx = new ParserContext("[]");
        flow.start(ctx);

        assertEquals("Final state should be DONE", DONE, ctx.getState());
        assertEquals("Result Item type should be List", Item.ItemType.LIST, ctx.getResult().getType());
        assertTrue("Result List should be empty", ctx.getResult().getList().isEmpty());
    }

    @Test
    public void testOneElement() {
        ParserContext ctx = new ParserContext("['Item 1']");
        flow.start(ctx);

        assertEquals("Final state should be DONE", DONE, ctx.getState());
        assertEquals("Result Item type should be List", Item.ItemType.LIST, ctx.getResult().getType());
        assertEquals("Result Item should have 1 sub-item", 1, ctx.getResult().getList().size());
        Item item0 = ctx.getResult().getList().get(0);
        assertEquals("Result Item[0] type should be Value", Item.ItemType.VALUE, item0.getType());
        assertEquals("Result Item[0] should be 'Item 1'", "Item 1", item0.getValue());
    }

    @Test
    public void testTwoElements() {
        ParserContext ctx = new ParserContext("['Item 1', 'Item 2']");
        flow.start(ctx);

        assertEquals("Final state should be DONE", DONE, ctx.getState());
        assertEquals("Result Item type should be List", Item.ItemType.LIST, ctx.getResult().getType());
        assertEquals("Result Item should have 2 sub-items", 2, ctx.getResult().getList().size());
        Item item1 = ctx.getResult().getList().get(1);
        assertEquals("Result Item[1] type should be Value", Item.ItemType.VALUE, item1.getType());
        assertEquals("Result Item[1] should be 'Item 2'", "Item 2", item1.getValue());
    }

    @Test
    public void testTwoElements2() {
        ParserContext ctx = new ParserContext("[,'Item 1', , 'Item 2', ]");
        flow.start(ctx);

        assertEquals("Final state should be DONE", DONE, ctx.getState());
        assertEquals("Result Item type should be List", Item.ItemType.LIST, ctx.getResult().getType());
        assertEquals("Result Item should have 2 sub-items", 2, ctx.getResult().getList().size());
        Item item1 = ctx.getResult().getList().get(1);
        assertEquals("Result Item[1] type should be Value", Item.ItemType.VALUE, item1.getType());
        assertEquals("Result Item[1] should be 'Item 2'", "Item 2", item1.getValue());
    }

    @Test
    public void testNested() {
        ParserContext ctx = new ParserContext("['Item 1', ['Item 3', 'Item 4'], 'Item 2']");
        flow.start(ctx);

        assertEquals("Final state should be DONE", DONE, ctx.getState());
        assertEquals("Result Item should have 3 sub-items", 3, ctx.getResult().getList().size());
        Item item0 = ctx.getResult().getList().get(0);
        assertEquals("Result Item[0] should be 'Item 1'", "Item 1", item0.getValue());
        Item item1 = ctx.getResult().getList().get(1);
        assertEquals("Result Item[1] should have 2 sub-items", 2, item1.getList().size());
        Item item2 = ctx.getResult().getList().get(2);
        assertEquals("Result Item[2] should be 'Item 2'", "Item 2", item2.getValue());

        Item item3 = item1.getList().get(0);
        assertEquals("Result Item[1][0] should be 'Item 3'", "Item 3", item3.getValue());
        Item item4 = item1.getList().get(1);
        assertEquals("Result Item[1][1] should be 'Item 4'", "Item 4", item4.getValue());
    }

    @Test
    public void testInvalidUnclosedArray() {
        ParserContext ctx = new ParserContext("['Item 1'");
        flow.start(ctx);

        assertEquals("Final state should be ERROR", ERROR, ctx.getState());
        assertNull("Result should be null", ctx.getResult());
    }

    @Test
    public void testInvalidUnclosedArray2() {
        ParserContext ctx = new ParserContext("['Item 1', []");
        flow.start(ctx);

        assertEquals("Final state should be ERROR", ERROR, ctx.getState());
        assertNull("Result should be null", ctx.getResult());
    }

    @Test
    public void testInvalidUnclosedArray3() {
        ParserContext ctx = new ParserContext("['Item 1', ['Item 2']");
        flow.start(ctx);

        assertEquals("Final state should be ERROR", ERROR, ctx.getState());
        assertNull("Result should be null", ctx.getResult());
    }

    @Test
    public void testInvalidUnclosedValue() {
        ParserContext ctx = new ParserContext("['Item 1]");
        flow.start(ctx);

        assertEquals("Final state should be ERROR", ERROR, ctx.getState());
        assertNull("Result should be null", ctx.getResult());
    }
}
