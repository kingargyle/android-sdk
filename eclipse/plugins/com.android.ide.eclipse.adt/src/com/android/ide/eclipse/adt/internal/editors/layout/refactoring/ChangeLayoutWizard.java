/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.ide.eclipse.adt.internal.editors.layout.refactoring;

import static com.android.ide.common.layout.LayoutConstants.FQCN_RELATIVE_LAYOUT;
import static com.android.ide.common.layout.LayoutConstants.RELATIVE_LAYOUT;

import com.android.ide.eclipse.adt.internal.editors.layout.LayoutEditor;

import org.eclipse.core.resources.IProject;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import java.util.List;

class ChangeLayoutWizard extends VisualRefactoringWizard {

    public ChangeLayoutWizard(ChangeLayoutRefactoring ref, LayoutEditor editor) {
        super(ref, editor);
        setDefaultPageTitle("Change Layout");
    }

    @Override
    protected void addUserInputPages() {
        ChangeLayoutRefactoring ref = (ChangeLayoutRefactoring) getRefactoring();
        String oldType = ref.getOldType();
        addPage(new InputPage(mEditor.getProject(), oldType));
    }

    /** Wizard page which inputs parameters for the {@link ChangeLayoutRefactoring} operation */
    private static class InputPage extends UserInputWizardPage {
        private final IProject mProject;
        private final String mOldType;
        private Combo mTypeCombo;
        private Button mFlatten;
        private List<String> mClassNames;

        public InputPage(IProject project, String oldType) {
            super("ChangeLayoutInputPage");  //$NON-NLS-1$
            mProject = project;
            mOldType = oldType;
        }

        public void createControl(Composite parent) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setLayout(new GridLayout(2, false));

            Label fromLabel = new Label(composite, SWT.NONE);
            fromLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
            String oldTypeBase = mOldType.substring(mOldType.lastIndexOf('.') + 1);
            fromLabel.setText(String.format("Change from %1$s", oldTypeBase));

            Label typeLabel = new Label(composite, SWT.NONE);
            typeLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
            typeLabel.setText("New Layout Type:");

            mTypeCombo = new Combo(composite, SWT.READ_ONLY);
            mTypeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
            SelectionAdapter selectionListener = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    validatePage();
                    // Hierarchy flattening only works for relative layout (and any future
                    // layouts that can also support arbitrary layouts).
                    mFlatten.setVisible(mTypeCombo.getText().equals(FQCN_RELATIVE_LAYOUT));
                }
            };
            mTypeCombo.addSelectionListener(selectionListener);

            mFlatten = new Button(composite, SWT.CHECK);
            mFlatten.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER,
                    false, false, 2, 1));
            mFlatten.setText("Flatten hierarchy");
            mFlatten.addSelectionListener(selectionListener);
            // Should flattening be selected by default?
            mFlatten.setSelection(true);

            // We don't exclude RelativeLayout even if the current layout is RelativeLayout,
            // in case you are trying to flatten the hierarchy for a hierarchy that has a
            // RelativeLayout at the root.
            boolean oldIsRelativeLayout = mOldType.equals(FQCN_RELATIVE_LAYOUT);
            String exclude = oldIsRelativeLayout ? null : mOldType;

            mClassNames = WrapInWizard.addLayouts(mProject, mOldType, mTypeCombo, exclude, false);

            mTypeCombo.select(0);
            // The default should be Relative layout, if available (and not the old Type)
            if (!oldIsRelativeLayout) {
                for (int i = 0; i < mTypeCombo.getItemCount(); i++) {
                    if (mTypeCombo.getItem(i).equals(RELATIVE_LAYOUT)) {
                        mTypeCombo.select(i);
                        break;
                    }
                }
            }
            mFlatten.setVisible(mTypeCombo.getText().equals(RELATIVE_LAYOUT));

            setControl(composite);
            validatePage();
        }

        private boolean validatePage() {
            boolean ok = true;

            int selectionIndex = mTypeCombo.getSelectionIndex();
            String type = selectionIndex != -1 ? mClassNames.get(selectionIndex) : null;
            if (type == null) {
                setErrorMessage("Select a layout type");
                ok = false; // The user has chosen a separator
            } else {
                setErrorMessage(null);

                // Record state
                ChangeLayoutRefactoring refactoring =
                    (ChangeLayoutRefactoring) getRefactoring();
                refactoring.setType(type);
                refactoring.setFlatten(mFlatten.getSelection());
            }

            setPageComplete(ok);
            return ok;
        }
    }
}