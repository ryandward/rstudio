/*
 * ToolbarPopupMenu.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;

import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.MenuItemSeparator;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuItem;
import org.rstudio.core.client.command.BaseMenuBar;
import org.rstudio.core.client.theme.res.ThemeStyles;

public class ToolbarPopupMenu extends ThemedPopupPanel
{
   // Extensibility point for dynamically constructed popup menus. The default
   // implementation returns itself, but extensions can do some work to build
   // the menu and return the built menu. Callers can use this in combination
   // with getDynamicPopupMenu() when an up-to-date instance of the object is
   // required.
   public interface DynamicPopupMenuCallback
   {
      public void onPopupMenu(ToolbarPopupMenu menu);
   }
   
   public interface SearchHandler
   {
      public void onQuery(String query);
   }

   public ToolbarPopupMenu()
   {
      super(true);
      
      menuBar_ = createMenuBar();
      searchWidget_ = new SearchWidget();
      searchWidget_.addStyleName(ThemeStyles.INSTANCE.searchBox());
      searchWidget_.setWidth(100, Unit.PCT);
      searchWidget_.setVisible(isSearchEnabled());
      
      Widget mainWidget = createMainWidget();
      
      if (isSearchEnabled())
      {
         VerticalPanel container = new VerticalPanel();
         container.add(searchWidget_);
         container.add(new HTML("<hr>"));
         container.add(mainWidget);
         setWidget(container);
      }
      else
      {
         setWidget(mainWidget);
      }
   }
   
   public ToolbarPopupMenu(ToolbarPopupMenu parent)
   {
      this();
      parent_ = parent;
   }

   protected ToolbarMenuBar createMenuBar()
   {
      return new ToolbarMenuBar(true);
   }

   protected Widget createMainWidget()
   {
      return menuBar_;
   }
   
   public boolean isSearchEnabled()
   {
      return false;
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      searchWidget_.setValue(null);
      menuBar_.selectItem(null);
   }
   
   public void selectFirst()
   {
      menuBar_.selectFirst();
   }

   public void selectItem(MenuItem menuItem)
   {
      menuBar_.selectItem(menuItem);
   }

   public void addItem(MenuItem menuItem)
   {
      ScheduledCommand command = menuItem.getScheduledCommand();
      if (command == null && menuItem instanceof AppMenuItem)
         command = ((AppMenuItem)menuItem).getScheduledCommand(true);
      if (command != null)
         menuItem.setScheduledCommand(new ToolbarPopupMenuCommand(command));
      menuBar_.addItem(menuItem);
   }
   
   public void addItem(SafeHtml html, MenuBar popup)
   {
      menuBar_.addItem(html, popup);
   }
   
   public void addItem(MenuItem menuItem, final ToolbarPopupMenu popup)
   {
      menuBar_.addItem(SafeHtmlUtils.fromTrustedString(menuItem.getHTML()), popup.menuBar_);
   }
   
   public void addItem(AppCommand command, ToolbarPopupMenu popup)
   {
      if (command.isEnabled())
         addItem(command.createMenuItem(false), popup);
   }
   
   public void setAutoOpen(boolean autoOpen)
   {
      menuBar_.setAutoOpen(autoOpen);
   }
   
   public void insertItem(MenuItem menuItem, int beforeIndex)
   {
     ScheduledCommand command = menuItem.getScheduledCommand() ;
      if (command != null)
         menuItem.setScheduledCommand(new ToolbarPopupMenuCommand(command));
      menuBar_.insertItem(menuItem, beforeIndex) ;
   }
   
   public void removeItem(MenuItem menuItem)
   {
      menuBar_.removeItem(menuItem) ;
   }
   
   public boolean containsItem(MenuItem menuItem)
   {
      return menuBar_.getItemIndex(menuItem) >= 0 ;
   }
   
   public void clearItems()
   {
      menuBar_.clearItems() ;
   }
   
   public void addSeparator()
   {
      menuBar_.addSeparator();
   }
   
   public void addSeparator(MenuItemSeparator separator)
   {
      menuBar_.addSeparator(separator);
   }
   
   public void addSeparator(String label)
   {
      menuBar_.addSeparator(new LabelledMenuSeparator(label));
   }
   
   public void addSeparator(int minPx)
   {
      menuBar_.addSeparator(new MinWidthMenuSeparator(minPx));
   }
   
   public int getItemCount()
   {
      return menuBar_.getItemCount() ;
   }

   public void focus()
   {
      menuBar_.focus();
   }
   
   public void setAutoHideRedundantSeparators(boolean value)
   {
      menuBar_.setAutoHideRedundantSeparators(value);
   }

   public void getDynamicPopupMenu(DynamicPopupMenuCallback callback)
   {
      callback.onPopupMenu(this);
   }

   private class ToolbarPopupMenuCommand implements ScheduledCommand
   {
      public ToolbarPopupMenuCommand(ScheduledCommand coreCommand)
      {
         coreCommand_ = coreCommand;
      }
      public void execute()
      {
         Scheduler.get().scheduleFinally(coreCommand_);
         hide();
         if (parent_ != null) parent_.hide();
      }
   
      private ScheduledCommand coreCommand_;
   }
   
   protected class ToolbarMenuBar extends BaseMenuBar
   {
      public ToolbarMenuBar(boolean vertical)
      {
         super(vertical) ;
      }
      
      @Override
      protected void onAttach()
      {
         super.onAttach();
         
         if (isSearchEnabled())
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  searchWidget_.focus();
               }
            });
         }
      }

      @Override
      protected void onUnload()
      {
         nativePreviewReg_.removeHandler();
         super.onUnload();
      }

      @Override
      protected void onLoad()
      {
         super.onLoad();
         
         nativePreviewReg_ = Event.addNativePreviewHandler(new NativePreviewHandler()
         {
            public void onPreviewNativeEvent(NativePreviewEvent e)
            {
               if (e.getTypeInt() == Event.ONKEYDOWN)
               {
                  if (handleSearchKey(e.getNativeEvent()))
                     return;
                  
                  switch (e.getNativeEvent().getKeyCode())
                  {
                     case KeyCodes.KEY_ESCAPE:
                        e.cancel();
                        hide();
                        break;
                     case KeyCodes.KEY_DOWN:
                        e.cancel();
                        moveSelectionDown();
                        break;
                     case KeyCodes.KEY_UP:
                        e.cancel();
                        moveSelectionUp();
                        break;
                     case KeyCodes.KEY_PAGEDOWN:
                        e.cancel();
                        moveSelectionFwd(5);
                        break;
                     case KeyCodes.KEY_PAGEUP:
                        e.cancel();
                        moveSelectionBwd(5);
                        break;
                     case KeyCodes.KEY_HOME:
                        e.cancel();
                        selectFirst();
                        break;
                     case KeyCodes.KEY_END:
                        e.cancel();
                        selectLast();
                        break;
                     case KeyCodes.KEY_ENTER:
                        e.cancel();
                        final MenuItem menuItem = getSelectedItem();
                        if (menuItem != null)
                        {
                           NativeEvent evt = Document.get().createClickEvent(
                                 0,
                                 0,
                                 0,
                                 0,
                                 0,
                                 false,
                                 false,
                                 false,
                                 false);
                           menuItem.getElement().dispatchEvent(evt);
                        }
                        break;
                  }
               }
            }
         });
      }

      public int getItemCount()
      {
         return getItems().size() ;
      }
      
      public int getSelectedIndex()
      {
         MenuItem selectedMenuItem = getSelectedItem();
         List<MenuItem> menuItems = getItems();
         for (int i = 0; i<menuItems.size(); i++)
         {
            if (menuItems.get(i).equals(selectedMenuItem))
               return i;
         }
         return -1;
      }
      
      private void moveSelectionFwd(int numElements)
      {
         selectItem(getSelectedIndex() + numElements);
      }
      
      private void moveSelectionBwd(int numElements)
      {
         selectItem(getSelectedIndex() - numElements);
      }
      
      private void selectFirst()
      {
         selectItem(0);
      }
      
      private void selectLast()
      {
         selectItem(getItemCount());
      }
      
      private void selectItem(int index)
      {
         int count = getItemCount();
         
         if (count == 0) return;
         
         if (index < 0)
            index = 0;
         
         if (index >= count - 1)
            index = count - 1;
         
         List<MenuItem> items = getItems();
         selectItem(items.get(index));
      }

      private HandlerRegistration nativePreviewReg_;
   }
   
   public void setSearchEnabled(boolean enabled)
   {
      searchEnabled_ = enabled;
   }
   
   public HandlerRegistration addSearchValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return searchWidget_.addValueChangeHandler(handler);
   }
   
   private boolean handleSearchKey(NativeEvent event)
   {
      if (!searchEnabled_)
         return false;
    
      int keyCode = event.getKeyCode();
      if (!Character.isLetterOrDigit((char) keyCode))
         return false;
      
      if (searchWidget_.isVisible())
         return false;
      
      event.stopPropagation();
      event.preventDefault();
      
      String initialValue = String.valueOf((char) keyCode);
      if (!event.getShiftKey())
         initialValue = initialValue.toLowerCase();
      searchWidget_.setValue(initialValue);
      
      searchWidget_.setVisible(true);
      searchWidget_.focus();
      
      return true;
   }
   
   protected ToolbarMenuBar menuBar_;
   protected SearchWidget searchWidget_;
   private boolean searchEnabled_;
   private ToolbarPopupMenu parent_;
}
