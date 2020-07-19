let s:cpo_save = &cpo
set cpo&vim

let s:channel_id = 0

function! s:AddImport(import)
  let l:lineNumber = 1
  let l:maxLine = line('$')
  while l:lineNumber <= l:maxLine
    let l:line = getline(l:lineNumber)
    if l:line =~# '^import '
      call append(l:lineNumber - 1, 'import ' . a:import)
      return
    endif
    let l:lineNumber += 1
  endwhile
  call append(1, 'import ' . a:import)
endfunction

function! s:HandleGoToEvent(data) abort
  if has_key(a:data, 'file')
    execute 'edit ' . a:data.file
    execute 'goto ' . (a:data.offset + 1)
  else
    echo '[Vintellij] Definition not found.'
  endif
endfunction

function! s:HandleImportEvent(data) abort
  let l:imports = a:data.imports
  if empty(l:imports)
    echo '[Vintellij] No import candidates found.'
    return
  endif
  call fzf#run(fzf#wrap({
        \ 'source': l:imports,
        \ 'sink': function('s:AddImport')
        \ }))
endfunction

function! s:HandleOpenEvent(data) abort
  echo '[Vintellij] File successfully opened: ' . a:data.file
endfunction

function! s:HandleRefreshEvent(data) abort
  echo '[Vintellij] File successfully refreshed: ' . a:data.file
endfunction

function! s:GetCurrentOffset()
  return line2byte(line('.')) + col('.') - 1
endfunction

function! s:OnReceiveData(channel_id, data, event) abort
  let l:json_data = json_decode(a:data)
  if !l:json_data.success
    throw '[Vintellij] ' . l:json_data.message
  endif

  let l:handler = l:json_data.handler
  if l:handler ==# 'goto'
    call s:HandleGoToEvent(l:json_data.data)
  elseif l:handler ==# 'import'
    call s:HandleImportEvent(l:json_data.data)
  elseif l:handler ==# 'open'
    call s:HandleOpenEvent(l:json_data.data)
  elseif l:handler ==# 'refresh'
    call s:HandleRefreshEvent(l:json_data.data)
  else
    throw '[Vintellij] Invalid handler: ' . l:handler
  endif
endfunction

function! s:IsConnected() abort
  return index(map(nvim_list_chans(), 'v:val.id'), s:channel_id) >= 0
endfunction

function! s:OpenConnection() abort
  try
    if exists('g:vintellij_port')
      let l:port = g:vintellij_port
    else
      let l:port = 6969
    endif
    let s:channel_id = sockconnect('tcp', 'localhost:' . l:port, {
          \                        'on_data': function('s:OnReceiveData'),
          \ })
  catch /connection failed/
    let s:channel_id = 0
  endtry

  return s:channel_id
endfunction

function! s:SendRequest(handler, data) abort
  if !s:IsConnected()
    if s:OpenConnection() == 0
      throw '[Vintellij] Can not connect to the plugin server. Please open intelliJ which is installed vintellij plugin.'
    endif
  endif
  call chansend(s:channel_id, json_encode({
        \ 'handler': a:handler,
        \ 'data': a:data
        \ }))
endfunction

function! vintellij#GoToDefinition() abort
  call s:SendRequest('goto', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#OpenFile() abort
  call s:SendRequest('open', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#SuggestImports() abort
  call s:SendRequest('import', {
        \ 'file': expand('%:p'),
        \ 'offset': s:GetCurrentOffset(),
        \ })
endfunction

function! vintellij#RefreshFile() abort
  call s:SendRequest('refresh', {
        \ 'file': expand('%:p'),
        \ })
endfunction

augroup on_kt_java_file_save 
  autocmd!
  autocmd BufWritePost,FileReadPost *.kt,*.java call vintellij#RefreshFile()
augroup END

let &cpo = s:cpo_save
unlet s:cpo_save

