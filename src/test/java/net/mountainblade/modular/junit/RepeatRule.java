/**
 * Copyright (C) 2014-2015 MountainBlade (http://mountainblade.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.mountainblade.modular.junit;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Represents a rule to execute tests in a loop.
 *
 * @author fappel
 */
public class RepeatRule implements TestRule {

    @Override
    public Statement apply( Statement statement, Description description ) {
        Statement result = statement;
        Repeat repeat = description.getAnnotation( Repeat.class );

        if( repeat != null ) {
            result = new RepeatStatement(repeat.value(), statement );
        }

        return result;
    }


    private static class RepeatStatement extends Statement {
        private final int times;
        private final Statement statement;

        private RepeatStatement( int times, Statement statement ) {
            this.times = times;
            this.statement = statement;
        }

        @Override
        public void evaluate() throws Throwable {
            for (int i = times; i > 0; i--) {
                System.out.println("\n\n---- Executing test ----");
                statement.evaluate();
            }
        }

    }

}
