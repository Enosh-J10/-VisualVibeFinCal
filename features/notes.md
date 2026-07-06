# NoteBook & Checklists

The NoteBook provides a local space for financial notes, shopping lists, and reminders.

## Persistence
- **Implementation**: Currently uses a dedicated `SharedPreferences` file: `NotesPrefs_$uid`.
- **Note Format**: Data is stored as a delimited string: `Title::::Content::::Type`.
- **Types**: `TEXT` or `CHECKLIST`.

## Behaviors
- **Checklist**: In checklist mode, line breaks (`\n`) are used to separate items. Checked status is persisted using `itemText::::status` sub-delimiters.
- **Sorting**: Notes are sorted by their ID (Timestamp) in descending order (Newest first).
- **Isolation**: Each user has an isolated notes set.

## Key Screens
- **`NoteBookScreen.kt`**: Main list and entry dialog.
- **`NoteItem`**: Component that renders the note body or the interactive checklist.

## Limitations
- Large notes (thousands of lines) may cause slight UI lag due to `SharedPreferences` synchronous read on load.
- Markdown is not supported within the NoteBook (Reserved for AI Chat).

## Development Guidelines
- Use the `AlertDialog` for adding/editing notes to maintain consistency with other tool dialogs.
- Always require confirmation before deleting a note.
