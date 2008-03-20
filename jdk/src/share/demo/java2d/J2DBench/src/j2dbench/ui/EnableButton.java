/*
 * Copyright 2003-2005 Sun Microsystems, Inc.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Sun Microsystems nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package j2dbench.ui;

import j2dbench.Group;
import j2dbench.Node;
import j2dbench.Option;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Insets;

public class EnableButton extends JButton implements ActionListener {
    public static final int SET = 0;
    public static final int CLEAR = 1;
    public static final int INVERT = 2;
    public static final int DEFAULT = 3;

    private Group group;
    private int type;

    public static final String icons[] = {
        "Set",
        "Clear",
        "Invert",
        "Default",
    };

    public EnableButton(Group group, int type) {
        super(icons[type]);
        this.group = group;
        this.type = type;
        addActionListener(this);
        setMargin(new Insets(0, 0, 0, 0));
        setBorderPainted(false);
    }

    public void actionPerformed(ActionEvent e) {
        Node.Iterator children = group.getRecursiveChildIterator();
        String newval = (type == SET) ? "enabled" : "disabled";
        while (children.hasNext()) {
            Node child = children.next();
            if (type == DEFAULT) {
                child.restoreDefault();
            } else if (child instanceof Option.Enable) {
                Option.Enable enable = (Option.Enable) child;
                if (type == INVERT) {
                    newval = enable.isEnabled() ? "disabled" : "enabled";
                }
                enable.setValueFromString(newval);
            }
        }
    }
}