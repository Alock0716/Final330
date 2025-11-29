# ShiftScheduler – Android Scheduling App
## CSC 330 • Final App Project • Android Studio (Java)

ShiftScheduler is a mobile scheduling system designed for small workplaces.
It helps managers create and update work schedules, and allows employees to view shifts, request time off, and trade shifts with coworkers. The app uses Firebase for real-time data synchronization so both managers and employees always see an up-to-date schedule.

### Features
####Employer / Manager Features

View the full schedule with Month, Week, and Day visual modes

Add, edit, and cancel shifts

Review pending trade requests

Review pending time-off requests

See shift status labels (Scheduled, Pending Trade, Called Off, Time-Off Approved)

Shifts that are canceled or approved for time off are removed from the active schedule

#### Employee Features

View personal upcoming shifts

Request time off from the app

Put a shift up for trade

Browse available shifts from other employees

Cancel a pending trade request

General App Features

Firebase database for persistent, synced storage

Multiple activities/screens with clear navigation

Error handling and null checking for safer runtime behavior

Input validation for shift requests and forms

Basic accessibility (labels, consistent touch targets, readable layouts)

### Data Model Overview
Users
Field	Description
userId	Firebase UID
name	Employee/Manager name
role	employer or employee
position	Optional job role
Shifts
Field	Description
shiftId	Unique ID
date, startTime, endTime	Shift schedule
employeeId	Assigned user
status	scheduled, pending_trade, called_off, time_off_approved
read	Boolean for update tracking
TradeRequests
Field	Description
requestId	Unique ID
shiftId	Linked shift
requestingEmployeeId	Who wants to trade
status	pending, accepted, denied
TimeOffRequests
Field	Description
requestId	Unique ID
employeeId	Who requested
startDate / endDate	Date range
status	pending, approved, denied

### Technologies Used

Android Studio (Java)

Firebase Realtime Database

Firebase Auth (for login UIDs)

RecyclerView + adapters

Explicit intents for navigation

Calendar-style layouts for schedule views

### Installation & Build Instructions
1. Clone or download the repository
git clone https://github.com/yourusername/ShiftScheduler.git

2. Open in Android Studio

Open Android Studio → Open an Existing Project

Select the cloned folder

3. Sync Gradle

Android Studio will automatically sync all dependencies, including Firebase.

4. Run the App

Connect an Android device or open a virtual emulator

Press Run 

The app will install and launch automatically

5. Firebase Setup (If Running Outside the Class Environment)

If someone else clones the project, they must add their own:

google-services.json

Firebase database rules

Authentication settings

### Release Notes (v1.0)

This release includes all required CSC-330 final project functionality:

Employer + Employee role separation

Shift creation, editing, cancellation

Month/Week/Day visual schedule

Shift trading system

Time-off requests with approval flow

Updated list items showing shift status

Firebase persistence

Basic error handling

No known blocking crashes

### Testing Summary

Manual testing performed on:

Pixel 6 Emulator (API 34)

Samsung Galaxy physical device (if applicable)

#### Tests covered:

Navigation between all screens

Creating/editing/canceling shifts

Submitting and approving time-off requests

Creating, canceling, and accepting trades

Verifying canceled/time-off-approved shifts no longer appear

Rotation handling on calendar/list screens

Firebase read/write operations

#### Known issues:

Slight UI delay if connection to Firebase is slow

Calendar visuals may shift slightly on older devices

Slight issue with some scheduled shifts duplicating in the DB however does not affect the App UI in any way

No current implementation for additional teams/businesses could be implemented very easily bt was not necessary for the project requirements.


### Developer:

Name: Aaron Lockhart
Semester: Fall 2025
Contact: Lockhaam2023@mountunion.edu
