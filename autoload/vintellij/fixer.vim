"=============================================================================
" AUTHOR:  beeender <chenmulong at gmail.com>
" License: GPLv3
"=============================================================================

function! vintellij#fixer#FixAtCursor() abort
    let l:buffer = bufnr('')
    let l:insight = vintellij#cursor#FindInsightAtCursor(l:buffer)

    if empty(l:insight) || empty(l:insight['fixers'])
        call vintellij#util#TruncatedEcho('No available fixer at current postion.')
        return
    endif

    let l:fix_list = ['Select the fixer, ENTER to apply the first one.']
    let l:idx = 0
    for l:text in l:insight['fixers']
        let l:fix_list = add(l:fix_list, '' . l:idx . ' ' . l:text)
        let l:idx = l:idx + 1
    endfor

    let l:selection = inputlist(l:fix_list)

    if (l:selection >= 0 && l:selection < len(l:fix_list))
        call vintellij#RequestQuickFix(l:buffer, l:insight['id'], l:selection)
    endif
endfunction
