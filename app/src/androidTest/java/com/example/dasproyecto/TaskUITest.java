package com.example.dasproyecto;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class TaskUITest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void testAddTaskAndVerifyDisplay() {
        // 1. Click on FAB to open AddTareaActivity
        onView(withId(R.id.fabAddTarea)).perform(click());

        // 2. Enter task details
        String taskTitle = "Test Task " + System.currentTimeMillis();
        String taskDesc = "This is a test description";
        
        onView(withId(R.id.etTitulo))
                .perform(typeText(taskTitle), closeSoftKeyboard());
        
        onView(withId(R.id.etDescripcion))
                .perform(typeText(taskDesc), closeSoftKeyboard());

        // 3. Save the task
        onView(withId(R.id.btnGuardar)).perform(click());

        // 4. Verify the task appears in the RecyclerView in MainActivity
        // We check if any item in the RecyclerView has the title we just entered
        onView(withId(R.id.recyclerViewTareas))
                .check(matches(hasDescendant(withText(taskTitle))));
    }
}
