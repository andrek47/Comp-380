# Comp-380
Keep Pace group project 

Why we made this app
- Our app is a minimalistic running helper designed to keep the user on pace without having to look at any screens.
why we are different 
- we wanted to create a pace keeper app that did not require the user to be on any device so that while
  they run they can stay vigilant and safe.
Example how to use it
- The user puts in their target pace then starts there run and Keep Pace moniters their run in real time
  making sure they don't stray away from their pace and if they do they get a buzz from their phone telling
  them they need to speed up or slow down.
What you need to run it
- location sharing, an andriod phone, YOU also must download a (to make it not accessible to anyone besides our team)

- Our set up - 
fire base handles all of our database needs as well as our authentication for logging into the app


- Our design pattern -
  Our app KeepPace uses the Adapter design pattern.
We use the adapter design to connect the app data such as miles ran and navigation logic to Android
user interface components in a clean and maintainable way. The reason we chose the Adapter pattern was because it allows us to 
seperate how data is stored and managed from how it is displayed on screen. For example, RunHistoryAdapter
adapts a list of RunModel methods into individual RecyclerView items, formatting distance, time, and points 
without changing the actual model.Also, LoginAdapter adapts tab positions into authentication 
for ViewPager2, allowing smooth navigation between login and signup screens. By using adapters, 
we avoid tightly grouping our Interface logic to data structures, making the code easier to extend, reuse, and maintain as 
the application grows and when we modifie it