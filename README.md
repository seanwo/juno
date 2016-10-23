# Juno (JupiterEd Website Monitor and Notifier)

Juno was named after the [Juno spacecraft](https://en.wikipedia.org/wiki/Juno_(spacecraft)) that is orbiting/monitoring Jupiter.

This command line application was designed to navigate the [JupiterEd](https://login.jupitered.com/login/) grades website and detect changes (per term) for a list of students.  Upon detecting an assignment change for the current term, the system is designed to send an email to a list of email addresses that shows which classes and which assignment have changed.

To setup the system you will need:

+ Access to a SMTP server (along with username and password) such as [smtp.gmail.com](https://support.google.com/a/answer/176600?hl=en)
+ The SMTP servers username and password used to send outgoing mail (typically your gmail credentials for above server).
+ The quick login url for your students grades in Jupiter
+ List of email addresses you want notified when assignments/grades change

*If you are going to use a gmail account to send notifications, I recommend setting up a seperate account to do this and not use your personal account.*

Build a juno.jar file using the included manifest.

Execute using:

```
java -jar juno.jar
```

When run for the first time it will create a sample config.xml file in a .juno directory off of your user home directory (~/.juno for linux %USERPROFILE%\.juno for windows).  Edit this file with the above specifics and set the polling interval.  An interval of 0, executes the code once and terminates.

Upon running it for the first time for any student (handles multiple students) it will store the current assignments off as a baseline and will not notify you until subsequent changes.



