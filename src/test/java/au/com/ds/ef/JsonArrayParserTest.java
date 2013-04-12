package au.com.ds.ef;

import au.com.ds.ef.call.StateHandler;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

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
            this.list = Lists.newArrayList();
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
            this.stack = Lists.newLinkedList();
            this.thisValue = Lists.newArrayList();
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

    private final State<ParserContext> GETTING_CHAR = FlowBuilder.state();
    private final State<ParserContext> DONE = FlowBuilder.state();
    private final State<ParserContext> ERROR = FlowBuilder.state();
    private final State<ParserContext> VALIDATING = FlowBuilder.state();
    private final State<ParserContext> PROCESSING_CHAR = FlowBuilder.state();
    private final State<ParserContext> STARTING_ARRAY = FlowBuilder.state();
    private final State<ParserContext> ENDING_ARRAY = FlowBuilder.state();
    private final State<ParserContext> STARTING_VALUE = FlowBuilder.state();
    private final State<ParserContext> CONTINUING_VALUE = FlowBuilder.state();
    private final State<ParserContext> ENDING_VALUE = FlowBuilder.state();
    private final State<ParserContext> PROCESSING_SPACE = FlowBuilder.state();

    private final Event<ParserContext> onNoMoreChars = FlowBuilder.event();
    private final Event<ParserContext> onNewChar = FlowBuilder.event();
    private final Event<ParserContext> onArrayStart = FlowBuilder.event();
    private final Event<ParserContext> onArrayEnd = FlowBuilder.event();
    private final Event<ParserContext> onValueStart = FlowBuilder.event();
    private final Event<ParserContext> onValueContinue = FlowBuilder.event();
    private final Event<ParserContext> onValueEnd = FlowBuilder.event();
    private final Event<ParserContext> onSpace = FlowBuilder.event();
    private final Event<ParserContext> onUnexpectedChar = FlowBuilder.event();
    private final Event<ParserContext> onCharProcessed = FlowBuilder.event();
    private final Event<ParserContext> onContentValid = FlowBuilder.event();
    private final Event<ParserContext> onContentInvalid = FlowBuilder.event();

    private EasyFlow<ParserContext> flow;

    @Before
    public void setUp() {
        flow = FlowBuilder.
            from(GETTING_CHAR).transit(
                onNewChar.to(PROCESSING_CHAR).transit(
                    onArrayStart.to(STARTING_ARRAY).transit(
                        onCharProcessed.to(GETTING_CHAR)
                    ),
                    onArrayEnd.to(ENDING_ARRAY).transit(
                        onCharProcessed.to(GETTING_CHAR),
                        onUnexpectedChar.finish(ERROR)
                    ),
                    onValueStart.to(STARTING_VALUE).transit(
                        onCharProcessed.to(GETTING_CHAR)
                    ),
                    onValueContinue.to(CONTINUING_VALUE).transit(
                        onCharProcessed.to(GETTING_CHAR)
                    ),
                    onValueEnd.to(ENDING_VALUE).transit(
                        onCharProcessed.to(GETTING_CHAR)
                    ),
                    onSpace.to(PROCESSING_SPACE).transit(
                        onCharProcessed.to(GETTING_CHAR)
                    ),
                    onUnexpectedChar.finish(ERROR)
                ),
                onNoMoreChars.to(VALIDATING).transit(
                    onContentValid.finish(DONE),
                    onContentInvalid.finish(ERROR)
                )
        )
        .executor(new SyncExecutor());

        GETTING_CHAR.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                if (context.getNextChar() == 0) {
                    onNoMoreChars.trigger(context);
                } else {
                    onNewChar.trigger(context);
                }
            }
        });

        PROCESSING_CHAR.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                char c = context.getThisChar();
                if (context.isStartedValue() && c != '\'') {
                    onValueContinue.trigger(context);
                    return;
                }

                switch (c) {
                    case '[':
                        onArrayStart.trigger(context);
                        break;
                    case ']':
                        onArrayEnd.trigger(context);
                        break;
                    case '\'':
                        if (context.isStartedValue()) {
                            onValueEnd.trigger(context);
                        } else {
                            onValueStart.trigger(context);
                        }
                        break;
                    case ' ':
                    case ',':
                        onSpace.trigger(context);
                        break;
                    default:
                        onUnexpectedChar.trigger(context);
                }
            }
        });

        STARTING_ARRAY.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                context.pushStack();
                onCharProcessed.trigger(context);
            }
        });

        ENDING_ARRAY.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                if (context.getStackLevel() > 0) {
                    context.popStack();
                    onCharProcessed.trigger(context);
                } else {
                    onUnexpectedChar.trigger(context);
                }
            }
        });

        STARTING_VALUE.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                context.startValue();
                onCharProcessed.trigger(context);
            }
        });

        CONTINUING_VALUE.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                context.appendToValue();
                onCharProcessed.trigger(context);
            }
        });

        ENDING_VALUE.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                context.endValue();
                onCharProcessed.trigger(context);
            }
        });

        PROCESSING_SPACE.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                onCharProcessed.trigger(context);
            }
        });

        VALIDATING.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
                if (context.getStackLevel() != 0) {
                    onContentInvalid.trigger(context);
                } else if (context.isStartedValue()) {
                    onContentInvalid.trigger(context);
                } else {
                    onContentValid.trigger(context);
                }
            }
        });

        ERROR.whenEnter(new StateHandler<ParserContext>() {
            @Override
            public void call(State<ParserContext> state, ParserContext context) throws Exception {
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
