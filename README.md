# Vintellij
Make IntelliJ as a server language protocol.

## Installation

### Vim

#### Dependencies
- [fzf](https://github.com/junegunn/fzf.vim)

#### Install

```
Plug 'dinhhuy258/vintellij', {'branch': 'comrade'}
```
The default mapping is
- <Leader>cgd to go to the definition from the symbol under cursor
- <Leader>cgh to find the hierarchies for the symbol under cursor
- <Leader>cgu to find all of the occurrences of the symbol under cursor
- <Leader>co to open the current file from vim by intelliJ
- <Leader>ci to suggest possible imports by the symbol under cursor

if you want to make your own custom keymap, then put the following in your `.vimrc`

```
let g:vintellij_use_default_keymap = 0
nnoremap <Leader>gcd :VintellijGoToDefinition<CR>
nnoremap <Leader>co :VintellijOpenFile<CR>
nnoremap <Leader>ci :VintellijSuggestImports<CR>
...
```

For better syntax support

```
Plug 'udalov/kotlin-vim'
```
### Intellij plugin

1. Import the project into Intellij
2. Modify intellij plugin version and intellij kotlin version in `build.gradle` file based on your version of Intellij
3. Run `gradle buildPlugin`
4. Install your plugin into Intellij. (Preferences -> Plugins -> Install Plugin from Disk...)

## Run IntelliJ in headless mode

Create the following script file `vintellj.sh`

```sh
#!/bin/sh

IDE_BIN_HOME="/Applications/IntelliJ IDEA CE.app/Contents/MacOS"
exec "$IDE_BIN_HOME/idea" vintellij-inspect "$@"
```
Execute the script to run IntelliJ in headless mode

```console
./vintellij.sh ${project-path}
```

## Vim functions

- `vintellij#SuggestImports()`: suggest a list of importable classes for an element at the current cursor

- `vintellij#GoToDefinition()`: go to the position where an element at the current cursor is defined

- `vintellij#FindHierarchy()`: go to the subclasses or overriding methods for the class/method at the current cursor (project scope only)

- `vintellij#FindUsage()`: list all the occurrences of the symbol at the current cursor (project scope only)

- `vintellij#OpenFile()`: open the current file in Intellij

## Features

| Name | Kotlin | Java |
| ---- | ------ | ---- |
| Go to definition | :white_check_mark: | :white_check_mark: |
| Suggest imports | :white_check_mark: | :white_check_mark: |
| Find hierarchies | :white_check_mark: | :white_check_mark: |
| Find usages | :white_check_mark: | :white_check_mark: |
| Auto complete | :white_check_mark: | :white_check_mark: |

## Vintellij versions

- `master`: This is an original version of vintellij
- `comrade`: This is currently the main branch of vintellij which is already integrated with comrade plugin
- `lsp`: This branch is in development

## Credits

- [Comrade](https://github.com/beeender/Comrade)
- [ComradeNeovim](https://github.com/beeender/ComradeNeovim)

Recently I just integrated the Comrade IntelliJ plugin source code into my plugin. It makes the vintelliJ plugin stronger and smarter. I would like to say thank to the author of the Comrade plugin for such a wonderful plugin that he made.

## Issue

- To make it work, the Intellij must open the same project as Vim.
- Always open Intellij otherwise everything will be slow - the workarround maybe:
  - Get IntelliJ focused by having it in your secondary screen
  - Get vim transparent and putting IntelliJ behind
  - Open IntelliJ in headless mode
  
## Roadmap

- Support language server protocol
