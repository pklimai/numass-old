/*
 * Copyright 2015 Alexander Nozik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package inr.numass

import groovy.transform.CompileStatic
import hep.dataforge.grind.WorkspaceSpec
import inr.numass.tasks.NumassFitScanSummaryTask
import inr.numass.tasks.NumassFitScanTask
import inr.numass.tasks.NumassPrepareTask
import inr.numass.tasks.NumassTableFilterTask

/**
 * Created by darksnake on 16-Aug-16.
 */
@CompileStatic
class NumassWorkspaceSpec extends WorkspaceSpec {

    NumassWorkspaceSpec() {
        //load tasks
        super.loadTask(NumassPrepareTask)
        super.loadTask(NumassTableFilterTask)
        super.loadTask(NumassFitScanTask)
        super.loadTask(NumassFitScanSummaryTask)
    }


}
