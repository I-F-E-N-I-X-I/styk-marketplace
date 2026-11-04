let csrf;
export async function api(path,method='GET',body){
  if(method!=='GET'&&!csrf)csrf=await fetch('/api/auth/csrf',{credentials:'same-origin'}).then(r=>r.json());
  const res=await fetch('/api'+path,{method,credentials:'same-origin',headers:{...(body!==undefined?{'Content-Type':'application/json'}:{}),...(method!=='GET'?{[csrf.header]:csrf.token}:{})},body:body===undefined?undefined:JSON.stringify(body)});
  if(!res.ok){let data;try{data=await res.json();}catch{data={message:'Сервер временно недоступен'};}if(res.status===403)csrf=null;throw Object.assign(new Error(data.message||'Не удалось выполнить запрос'),{status:res.status});}
  return res.status===204||res.headers.get('content-length')==='0'?null:res.text().then(t=>t?JSON.parse(t):null);
}
export function clearCsrf(){csrf=null;}
