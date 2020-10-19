"=============================================================================
" AUTHOR:  beeender <chenmulong at gmail.com>
" License: GPLv3
"=============================================================================


function! vintellij#events#Init() abort
    augroup ComradeEvents
        autocmd!

        autocmd BufEnter * call vintellij#buffer#Notify()
        autocmd BufUnload * call vintellij#buffer#UnregisterCurrent()

        autocmd CursorMoved,CursorHold * call vintellij#cursor#EchoCursorWarningWithDelay()
        " Look for a warning to echo as soon as we leave Insert mode.
        " The script's position variable used when moving the cursor will
        " not be changed here.
        autocmd InsertLeave * call vintellij#cursor#EchoCursorWarning()
    augroup END
endfunction

function! vintellij#events#RegisterAutoImportOnCompletionDone() abort
  augroup vintellij_autoimport_when_completiondone
    autocmd!
    autocmd CompleteDone *.{java,kt,kts} call vintellij#autoimport#OnCompletionDone()
  augroup END
endfunction
