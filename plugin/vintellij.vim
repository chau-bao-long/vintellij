command! VintellijGoToDefinition call vintellij#GoToDefinition()
command! VintellijOpenFile call vintellij#OpenFile()
command! VintellijSuggestImports call vintellij#SuggestImports()
command! VintellijFindHierarchy call vintellij#FindHierarchy()
command! VintellijFindUsage call vintellij#FindUsage()

if get(g:, 'vintellij_use_default_keymap', 1) == 1
  nnoremap <Leader>cgd :VintellijGoToDefinition<CR>
  nnoremap <Leader>cgh :VintellijFindHierarchy<CR>
  nnoremap <Leader>cgu :VintellijFindUsage<CR>
  nnoremap <Leader>co :VintellijOpenFile<CR>
  nnoremap <Leader>ci :VintellijSuggestImports<CR>
endif

if get(g:, 'huy_duong_workspace', 0) == 1
  let g:vintellij_acceptable_num_candidates = 15
endif


"=============================================================================
" AUTHOR:  beeender <chenmulong at gmail.com>
" License: GPLv3
"=============================================================================

let s:comrade_major_version = 1
let s:comrade_minor_version = 0
let s:comrade_patch_version = 0
let g:comrade_version = s:comrade_major_version .
      \ '.' . s:comrade_minor_version .
      \ '.' . s:comrade_patch_version
let g:comrade_enabled = get(g:, 'comrade_enabled', v:false)

let s:path = fnamemodify(resolve(expand('<sfile>:p')), ':h')
let s:init_path = s:path . '/vintellij_init.py'

function! s:VintellijComradeEnable() abort
  if exists('s:comrade_loaded')
    return
  endif
  let s:comrade_loaded = 1

  exe 'py3file' s:init_path
  call vintellij#events#Init()
  call vintellij#events#RegisterAutoImportOnCompletionDone()

  let s:comrade_loaded = v:true
  echomsg "Vintellij comrade: ON"
endfunction

command! -nargs=0 VintellijQuickFix call vintellij#fixer#FixAtCursor()
command! VintellijComradeEnable call <SID>VintellijComradeEnable()
